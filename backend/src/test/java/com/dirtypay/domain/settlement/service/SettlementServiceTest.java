package com.dirtypay.domain.settlement.service;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.domain.settlement.dto.response.MemberAmountResponse;
import com.dirtypay.domain.settlement.dto.response.MemberSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.NodeOrdersResponse;
import com.dirtypay.domain.settlement.dto.response.NodeSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.OrderGroupResponse;
import com.dirtypay.domain.settlement.dto.response.OrderMemberShareResponse;
import com.dirtypay.domain.settlement.dto.response.OrderSettlementItemResponse;
import com.dirtypay.domain.settlement.dto.response.OrderSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.RoundSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.SessionSettlementResponse;
import com.dirtypay.domain.settlement.entity.SettlementPayment;
import com.dirtypay.domain.settlement.repository.SettlementPaymentRepository;
import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundParticipantRepository roundParticipantRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private OrgMemberService orgMemberService;

    @Mock
    private SettlementPaymentRepository settlementPaymentRepository;

    @Mock
    private RoundGroupRepository roundGroupRepository;

    @Mock
    private RoundGroupMemberRepository roundGroupMemberRepository;

    @Nested
    @DisplayName("라운드 정산 테스트")
    class CalculateRoundSettlementTest {

        @Test
        @DisplayName("10,000원을 3명에게 OWNER 전략으로 정산하면 총무가 나머지를 부담한다")
        void calculateRoundSettlement_ownerStrategy() {
            // given
            Long roundId = 1L;
            Long sessionId = 1L;
            Long ownerId = 1L;
            Long groupId = 10L;

            Round round = createRound(roundId, sessionId);
            Session session = createSession(sessionId, ownerId);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);
            RoundParticipant p3 = createParticipant(3L, roundId, 3L);

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2, p3));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L),
                            createRoundGroupMember(3L, groupId, 3L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희", 3L, "민수"));

            // when
            RoundSettlementResponse response = settlementService
                    .calculateRoundSettlement(roundId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
            assertThat(response.getSettlements()).hasSize(3);

            BigDecimal sum = response.getSettlements().stream()
                    .map(s -> s.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum).isEqualByComparingTo(new BigDecimal("10000"));

            // 라운드 정산에서는 완료 상태가 기본값
            response.getSettlements().forEach(s -> {
                assertThat(s.isPaid()).isFalse();
                assertThat(s.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            });
        }

        @Test
        @DisplayName("제외된 멤버는 0원으로 정산된다")
        void calculateRoundSettlement_excludedMember() {
            // given
            Long roundId = 1L;
            Long sessionId = 1L;
            Long groupId = 10L;

            Round round = createRound(roundId, sessionId);
            Session session = createSession(sessionId, 1L);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);
            p2.exclude();

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            RoundSettlementResponse response = settlementService
                    .calculateRoundSettlement(roundId, RemainderStrategyType.OWNER);

            // then
            var excludedSettlement = response.getSettlements().stream()
                    .filter(s -> s.getOrgMemberId().equals(2L))
                    .findFirst()
                    .orElseThrow();

            assertThat(excludedSettlement.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(excludedSettlement.isExcluded()).isTrue();
        }
    }


    @Nested
    @DisplayName("세션 정산 완료 상태 조회 테스트")
    class CalculateSessionSettlementWithPaymentTest {

        @Test
        @DisplayName("세션 정산 조회 시 납부 정보가 포함된다")
        void calculateSessionSettlement_includesPaymentInfo() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;
            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(sessionId)
                    .orgMemberId(2L)
                    .build();
            payment.updatePayment(new BigDecimal("3000"), new BigDecimal("5000"));

            List<Long> roundIds = List.of(roundId);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            // 배치 조회 스텁 (N+1 최적화 이후)
            given(roundParticipantRepository.findByRoundIdIn(roundIds))
                    .willReturn(List.of(p1, p2));
            given(orderRepository.findByRoundIdIn(roundIds))
                    .willReturn(List.of(order));
            given(roundGroupRepository.findByRoundIdIn(roundIds))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(anyList()))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));
            given(orgMemberRepository.findAllById(anyList()))
                    .willReturn(List.of());
            given(settlementPaymentRepository.findBySessionId(sessionId))
                    .willReturn(List.of(payment));

            // when
            SessionSettlementResponse response = settlementService
                    .calculateSessionSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            var member2Settlement = response.getSettlements().stream()
                    .filter(s -> s.getOrgMemberId().equals(2L))
                    .findFirst()
                    .orElseThrow();

            assertThat(member2Settlement.isPaid()).isFalse();
            assertThat(member2Settlement.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3000"));
            assertThat(member2Settlement.getRemainingAmount()).isPositive();
        }
    }

    @Nested
    @DisplayName("정산 완료 표시 테스트")
    class UpdateSettlementPaymentTest {

        @Test
        @DisplayName("전액 납부 시 isPaid가 true이고 remainingAmount가 0이다")
        void updateSettlementPayment_fullPayment() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 2L;
            Long roundId = 1L;

            setupMemberSettlementMocks(sessionId, roundId, orgMemberId, new BigDecimal("5000"));

            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(sessionId)
                    .orgMemberId(orgMemberId)
                    .build();

            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(Optional.of(payment));

            // when
            MemberSettlementResponse response = settlementService
                    .updateSettlementPayment(sessionId, orgMemberId,
                            new BigDecimal("5000"), RemainderStrategyType.OWNER);

            // then
            assertThat(response.isPaid()).isTrue();
            assertThat(response.getPaidAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(response.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("부분 납부 시 isPaid가 false이고 remainingAmount가 양수이다")
        void updateSettlementPayment_partialPayment() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 2L;
            Long roundId = 1L;

            setupMemberSettlementMocks(sessionId, roundId, orgMemberId, new BigDecimal("5000"));

            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(sessionId)
                    .orgMemberId(orgMemberId)
                    .build();

            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(Optional.of(payment));

            // when
            MemberSettlementResponse response = settlementService
                    .updateSettlementPayment(sessionId, orgMemberId,
                            new BigDecimal("3000"), RemainderStrategyType.OWNER);

            // then
            assertThat(response.isPaid()).isFalse();
            assertThat(response.getPaidAmount()).isEqualByComparingTo(new BigDecimal("3000"));
            assertThat(response.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("2000"));
        }

        @Test
        @DisplayName("음수 금액 시 예외가 발생한다")
        void updateSettlementPayment_negativeAmount_throwsException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 2L;
            Long roundId = 1L;

            setupMemberSettlementMocks(sessionId, roundId, orgMemberId, new BigDecimal("5000"));

            // when & then
            assertThatThrownBy(() -> settlementService
                    .updateSettlementPayment(sessionId, orgMemberId,
                            new BigDecimal("-1000"), RemainderStrategyType.OWNER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("납부 금액이 유효하지 않습니다");
        }

        @Test
        @DisplayName("총액 초과 금액 시 예외가 발생한다")
        void updateSettlementPayment_exceedingAmount_throwsException() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 2L;
            Long roundId = 1L;

            setupMemberSettlementMocks(sessionId, roundId, orgMemberId, new BigDecimal("5000"));

            // when & then
            assertThatThrownBy(() -> settlementService
                    .updateSettlementPayment(sessionId, orgMemberId,
                            new BigDecimal("6000"), RemainderStrategyType.OWNER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("납부 금액이 유효하지 않습니다");
        }

        @Test
        @DisplayName("기존 기록이 없으면 새로 생성한다")
        void updateSettlementPayment_createsNewPayment() {
            // given
            Long sessionId = 1L;
            Long orgMemberId = 2L;
            Long roundId = 1L;

            setupMemberSettlementMocks(sessionId, roundId, orgMemberId, new BigDecimal("5000"));

            SettlementPayment newPayment = SettlementPayment.builder()
                    .sessionId(sessionId)
                    .orgMemberId(orgMemberId)
                    .build();

            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, orgMemberId))
                    .willReturn(Optional.empty());
            given(settlementPaymentRepository.save(any(SettlementPayment.class)))
                    .willReturn(newPayment);

            // when
            MemberSettlementResponse response = settlementService
                    .updateSettlementPayment(sessionId, orgMemberId,
                            new BigDecimal("5000"), RemainderStrategyType.OWNER);

            // then
            assertThat(response.isPaid()).isTrue();
            assertThat(response.getPaidAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        }

        private void setupMemberSettlementMocks(Long sessionId, Long roundId,
                                                 Long orgMemberId, BigDecimal memberAmount) {
            Long groupId = 10L;
            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, orgMemberId);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, orgMemberId)));
        }
    }

    @Nested
    @DisplayName("주문 중심 정산 테스트")
    class CalculateOrderSettlementTest {

        @Test
        @DisplayName("같은 카테고리 2개 메뉴가 1그룹 2아이템으로 그룹핑된다")
        void calculateOrderSettlement_sameCategory_oneGroup() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order1 = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 2, new BigDecimal("30000"), "메인");
            Order order2 = createOrderWithGroup(2L, roundId, groupId, "그룹",
                    2L, "메뉴2", 1, new BigDecimal("14000"), "메인");

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getOrderGroups()).hasSize(1);
            assertThat(response.getOrderGroups().get(0).getCategory()).isEqualTo("메인");
            assertThat(response.getOrderGroups().get(0).getItems()).hasSize(2);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("44000"));
        }

        @Test
        @DisplayName("다른 카테고리 메뉴는 복수 그룹으로 분리된다")
        void calculateOrderSettlement_multipleCategories() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order1 = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("15000"), "메인");
            Order order2 = createOrderWithGroup(2L, roundId, groupId, "그룹",
                    2L, "메뉴2", 2, new BigDecimal("10000"), "주류");

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getOrderGroups()).hasSize(2);

            // 알파벳순: 메인 < 주류
            List<String> categories = response.getOrderGroups().stream()
                    .map(OrderGroupResponse::getCategory)
                    .toList();
            assertThat(categories).containsExactly("메인", "주류");
        }

        @Test
        @DisplayName("카테고리 없는 메뉴는 null 그룹으로 마지막에 위치한다")
        void calculateOrderSettlement_nullCategory_lastGroup() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order1 = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("15000"), "메인");
            Order order2 = createOrderWithGroup(2L, roundId, groupId, "그룹",
                    2L, "메뉴2", 1, new BigDecimal("8000"), null);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getOrderGroups()).hasSize(2);
            assertThat(response.getOrderGroups().get(0).getCategory()).isEqualTo("메인");
            assertThat(response.getOrderGroups().get(1).getCategory()).isNull();
            assertThat(response.getOrderGroups().get(1).getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("8000"));
        }

        @Test
        @DisplayName("균등 분배: 2명이 30,000원 주문 시 각 15,000원")
        void calculateOrderSettlement_equalDistribution() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 2, new BigDecimal("30000"), "메인");

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            List<OrderMemberShareResponse> members = response.getOrderGroups().get(0)
                    .getItems().get(0).getMembers();

            assertThat(members).hasSize(2);

            OrderMemberShareResponse member1 = members.stream()
                    .filter(m -> m.getOrgMemberId().equals(1L))
                    .findFirst().orElseThrow();
            OrderMemberShareResponse member2 = members.stream()
                    .filter(m -> m.getOrgMemberId().equals(2L))
                    .findFirst().orElseThrow();

            // 균등 분배: shareRatio=1, totalRatio=2
            assertThat(member1.getShareRatio()).isEqualTo(1);
            assertThat(member1.getTotalRatio()).isEqualTo(2);
            assertThat(member1.getAmount()).isEqualByComparingTo(new BigDecimal("15000"));

            assertThat(member2.getShareRatio()).isEqualTo(1);
            assertThat(member2.getTotalRatio()).isEqualTo(2);
            assertThat(member2.getAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("제외된 멤버는 미포함된다")
        void calculateOrderSettlement_excludedMember() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            Order order = createOrderWithGroup(1L, roundId, groupId, "그룹",
                    1L, "메뉴1", 1, new BigDecimal("15000"), "메인");

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);
            RoundParticipant p3 = createParticipant(3L, roundId, 3L);
            p3.exclude(); // member3 제외

            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2, p3));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L),
                            createRoundGroupMember(3L, groupId, 3L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            List<OrderMemberShareResponse> members = response.getOrderGroups().get(0)
                    .getItems().get(0).getMembers();

            // member3은 제외됨 → 2명만 포함
            assertThat(members).hasSize(2);
            assertThat(members.stream().map(OrderMemberShareResponse::getOrgMemberId).toList())
                    .containsExactlyInAnyOrder(1L, 2L);
            // totalRatio = 2 (member1 + member2)
            assertThat(members.get(0).getTotalRatio()).isEqualTo(2);
        }

        @Test
        @DisplayName("다중 라운드에서 roundId가 구분된다")
        void calculateOrderSettlement_multipleRounds() {
            // given
            Long sessionId = 1L;
            Long group1Id = 10L;
            Long group2Id = 20L;

            Session session = createSession(sessionId, 1L);
            Round round1 = createRound(1L, sessionId);
            Round round2 = createRound(2L, sessionId);

            Order order1 = createOrderWithGroup(1L, 1L, group1Id, "그룹1",
                    1L, "메뉴1", 1, new BigDecimal("15000"), "메인");
            Order order2 = createOrderWithGroup(2L, 2L, group2Id, "그룹2",
                    2L, "메뉴2", 1, new BigDecimal("20000"), "메인");

            RoundParticipant p1r1 = createParticipant(1L, 1L, 1L);
            RoundParticipant p1r2 = createParticipant(2L, 2L, 1L);

            RoundGroup g1 = createRoundGroup(group1Id, 1L, null, "그룹1", 0);
            RoundGroup g2 = createRoundGroup(group2Id, 2L, null, "그룹2", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round1, round2));
            given(orderRepository.findByRoundId(1L)).willReturn(List.of(order1));
            given(orderRepository.findByRoundId(2L)).willReturn(List.of(order2));
            given(roundParticipantRepository.findByRoundId(1L)).willReturn(List.of(p1r1));
            given(roundParticipantRepository.findByRoundId(2L)).willReturn(List.of(p1r2));
            given(roundGroupRepository.findByRoundId(1L)).willReturn(List.of(g1));
            given(roundGroupRepository.findByRoundId(2L)).willReturn(List.of(g2));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(group1Id)))
                    .willReturn(List.of(createRoundGroupMember(1L, group1Id, 1L)));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(group2Id)))
                    .willReturn(List.of(createRoundGroupMember(2L, group2Id, 1L)));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수"));

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("35000"));
            assertThat(response.getOrderGroups()).hasSize(1); // 같은 "메인" 카테고리

            List<OrderSettlementItemResponse> items = response.getOrderGroups().get(0).getItems();
            assertThat(items).hasSize(2);
            assertThat(items.stream().map(OrderSettlementItemResponse::getRoundId).toList())
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("빈 세션은 빈 orderGroups와 totalAmount=0을 반환한다")
        void calculateOrderSettlement_emptySession() {
            // given
            Long sessionId = 1L;

            Session session = createSession(sessionId, 1L);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of());

            // when
            OrderSettlementResponse response = settlementService
                    .calculateOrderSettlement(sessionId, RemainderStrategyType.OWNER);

            // then
            assertThat(response.getOrderGroups()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getStrategy()).isEqualTo(RemainderStrategyType.OWNER);
        }
    }

    @Nested
    @DisplayName("그룹 계층 기반 동적 분배 정산 테스트")
    class CalculateSettlementWithGroupHierarchyTest {

        // 데이터 구조:
        // 세션(id=1) → 라운드(id=1)
        // ├── 부모 그룹(id=1, shared): 멤버 555(1L)
        // │   └── 주문1: 삼겹살 100,000원 → 부모+자식 멤버에게 분배
        // └── 자식 그룹(id=2, 주류팀, parent=1): 멤버 ggg(2L)
        //     └── 주문2: 맥주 15,000원 → 자식 멤버에게만 분배

        private final Long roundId = 1L;
        private final Long sessionId = 1L;
        private final Long parentGroupId = 1L;
        private final Long childGroupId = 2L;

        private Round round;
        private Session session;
        private RoundGroup parentGroup;
        private RoundGroup childGroup;
        private Order order1; // 삼겹살 100,000원 (부모 그룹)
        private Order order2; // 맥주 15,000원 (자식 그룹)
        private RoundParticipant p1, p2;

        private void setupCommonData() {
            round = createRound(roundId, sessionId);
            session = createSession(sessionId, 1L);

            parentGroup = createRoundGroup(parentGroupId, roundId, null, "shared", 0);
            childGroup = createRoundGroup(childGroupId, roundId, parentGroupId, "주류팀", 1);

            // 부모 그룹 주문: 삼겹살 100,000원
            order1 = createOrderWithGroup(1L, roundId, parentGroupId, "shared",
                    1L, "삼겹살", 1, new BigDecimal("100000"), "메인");
            // 자식 그룹 주문: 맥주 15,000원
            order2 = createOrderWithGroup(2L, roundId, childGroupId, "주류팀",
                    2L, "맥주", 1, new BigDecimal("15000"), "주류");

            p1 = createParticipant(1L, roundId, 1L); // 555
            p2 = createParticipant(2L, roundId, 2L); // ggg
        }

        private void setupGroupHierarchyMocks() {
            // buildGroupDescendantMemberMap 용 mock
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(parentGroup, childGroup));
            given(roundGroupMemberRepository.findByGroupIdIn(anyList()))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, parentGroupId, 1L),  // 555 → 부모 그룹
                            createRoundGroupMember(2L, childGroupId, 2L))); // ggg → 자식 그룹
        }

        @Test
        @DisplayName("TC1: 라운드 정산 — 부모 주문(100K)은 전체, 자식 주문(15K)은 자식만")
        void calculateRoundSettlement_groupHierarchyDistribution() {
            // given
            setupCommonData();
            setupGroupHierarchyMocks();

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "555", 2L, "ggg"));

            // when
            RoundSettlementResponse response = settlementService
                    .calculateRoundSettlement(roundId, RemainderStrategyType.OWNER);

            // then
            // 부모 주문 100,000 → 555: 50,000 + ggg: 50,000
            // 자식 주문 15,000 → ggg: 15,000
            // 결과: 555=50,000, ggg=65,000
            assertThat(response.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("115000"));
            assertThat(response.getSettlements()).hasSize(2);

            BigDecimal member555 = findMemberAmount(response.getSettlements(), 1L);
            BigDecimal memberGgg = findMemberAmount(response.getSettlements(), 2L);
            assertThat(member555).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(memberGgg).isEqualByComparingTo(new BigDecimal("65000"));
        }

        @Test
        @DisplayName("TC2: 멤버 정산 — 부모 멤버는 부모 주문만 청구된다")
        void calculateMemberSettlement_parentMember_onlyParentOrder() {
            // given
            setupCommonData();
            setupGroupHierarchyMocks();

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));

            // when — 555(1L) 멤버 정산
            MemberSettlementResponse response = settlementService
                    .calculateMemberSettlement(sessionId, 1L, RemainderStrategyType.OWNER);

            // then — 부모 주문 100K / 2명 = 50,000원만
            assertThat(response.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(response.getDetails()).hasSize(1);
            assertThat(response.getDetails().get(0).getOrders()).hasSize(1);
        }

        @Test
        @DisplayName("TC3: 멤버 정산 — 자식 멤버는 부모+자식 주문 모두 청구된다")
        void calculateMemberSettlement_childMember_chargedForBoth() {
            // given
            setupCommonData();
            setupGroupHierarchyMocks();

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));

            // when — ggg(2L) 멤버 정산
            MemberSettlementResponse response = settlementService
                    .calculateMemberSettlement(sessionId, 2L, RemainderStrategyType.OWNER);

            // then — 부모 주문 50,000 + 자식 주문 15,000 = 65,000원
            assertThat(response.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("65000"));
            assertThat(response.getDetails()).hasSize(1);
            assertThat(response.getDetails().get(0).getOrders()).hasSize(2);
        }

        @Test
        @DisplayName("TC4: 상위 그룹 정산 — 자기 주문만 포함, 그룹 계층 기반 분배")
        void calculateGroupSettlement_parentGroup() {
            // given
            setupCommonData();
            setupGroupHierarchyMocks();

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orderRepository.findByRoundIdAndGroupIdIn(roundId, List.of(parentGroupId)))
                    .willReturn(List.of(order1));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "555", 2L, "ggg"));

            // when
            NodeSettlementResponse response = settlementService
                    .calculateGroupSettlement(roundId, parentGroupId, RemainderStrategyType.OWNER);

            // then — 부모 주문 100K → 555: 50K, ggg: 50K
            assertThat(response.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(response.getSettlements()).hasSize(2);

            BigDecimal member555 = findNodeMemberAmount(response.getSettlements(), 1L);
            BigDecimal memberGgg = findNodeMemberAmount(response.getSettlements(), 2L);
            assertThat(member555).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(memberGgg).isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("TC5: 하위 그룹 정산 — 상위+자기 주문, 자식 주문은 자식만")
        void calculateGroupSettlement_childGroup() {
            // given
            setupCommonData();
            setupGroupHierarchyMocks();

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(orderRepository.findByRoundIdAndGroupIdIn(roundId, List.of(childGroupId, parentGroupId)))
                    .willReturn(List.of(order1, order2));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "555", 2L, "ggg"));

            // when
            NodeSettlementResponse response = settlementService
                    .calculateGroupSettlement(roundId, childGroupId, RemainderStrategyType.OWNER);

            // then — 부모 주문(100K) + 자식 주문(15K) = 115,000원
            // 555: 50K(부모 주문만), ggg: 65K(부모50K + 자식15K)
            assertThat(response.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("115000"));

            BigDecimal member555 = findNodeMemberAmount(response.getSettlements(), 1L);
            BigDecimal memberGgg = findNodeMemberAmount(response.getSettlements(), 2L);
            assertThat(member555).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(memberGgg).isEqualByComparingTo(new BigDecimal("65000"));
        }

        @Test
        @DisplayName("TC6: excluded 멤버는 그룹 계층 분배에서 제외된다")
        void calculateRoundSettlement_excludedMember_notDistributed() {
            // given
            setupCommonData();
            p2.exclude(); // ggg 제외

            setupGroupHierarchyMocks();

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order1, order2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "555", 2L, "ggg"));

            // when
            RoundSettlementResponse response = settlementService
                    .calculateRoundSettlement(roundId, RemainderStrategyType.OWNER);

            // then — ggg 제외됨
            // 부모 주문 100K → descendant 멤버 중 active=[555] → 555: 100K
            // 자식 주문 15K → descendant 멤버 중 active=[] (ggg 제외) → skip
            // 총액 115K, 분배합 100K → 나머지 15K가 OWNER 전략에 의해 활성 멤버(555)에 가산
            BigDecimal member555 = findMemberAmount(response.getSettlements(), 1L);
            BigDecimal memberGgg = findMemberAmount(response.getSettlements(), 2L);
            assertThat(member555).isEqualByComparingTo(new BigDecimal("115000"));
            assertThat(memberGgg).isEqualByComparingTo(BigDecimal.ZERO);
        }

        private BigDecimal findMemberAmount(
                List<com.dirtypay.domain.settlement.dto.response.MemberAmountResponse> settlements,
                Long orgMemberId) {
            return settlements.stream()
                    .filter(s -> s.getOrgMemberId().equals(orgMemberId))
                    .findFirst()
                    .map(com.dirtypay.domain.settlement.dto.response.MemberAmountResponse::getAmount)
                    .orElseThrow(() -> new AssertionError("멤버 " + orgMemberId + " 정산 정보 없음"));
        }

        private BigDecimal findNodeMemberAmount(
                List<MemberAmountResponse> settlements, Long orgMemberId) {
            return settlements.stream()
                    .filter(s -> s.getOrgMemberId().equals(orgMemberId))
                    .findFirst()
                    .map(MemberAmountResponse::getAmount)
                    .orElseThrow(() -> new AssertionError("멤버 " + orgMemberId + " 정산 정보 없음"));
        }
    }

    @Nested
    @DisplayName("그룹 주문 조회 테스트")
    class GetGroupOrdersTest {

        @Test
        @DisplayName("카테고리별로 주문이 그룹핑되어 반환된다")
        void getGroupOrders_groupsByCategory() {
            // given
            Long roundId = 1L;
            Long groupId = 10L;

            Round round = createRound(roundId, 1L);
            RoundGroup group = createRoundGroup(groupId, roundId, null, "전체", 0);

            Order order1 = createOrderWithGroup(1L, roundId, groupId, "전체",
                    1L, "삼겹살", 1, new BigDecimal("15000"), "메인");
            Order order2 = createOrderWithGroup(2L, roundId, groupId, "전체",
                    2L, "맥주", 2, new BigDecimal("10000"), "주류");

            OrderDetail detail1 = createOrderDetail(1L, 1L, 1L);
            OrderDetail detail2 = createOrderDetail(2L, 2L, 1L);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(orderRepository.findByRoundIdAndGroupIdIn(roundId, List.of(groupId)))
                    .willReturn(List.of(order1, order2));
            given(orderDetailRepository.findByOrderIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(detail1, detail2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수"));

            // when
            NodeOrdersResponse response = settlementService.getGroupOrders(roundId, groupId);

            // then
            assertThat(response.getGroupId()).isEqualTo(groupId);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("25000"));
            assertThat(response.getCategories()).hasSize(2);
            assertThat(response.getCategories().stream()
                    .map(c -> c.getCategory()).toList())
                    .containsExactly("메인", "주류"); // 알파벳순 정렬
        }

        @Test
        @DisplayName("주문이 없는 그룹은 빈 응답을 반환한다")
        void getGroupOrders_emptyGroup() {
            // given
            Long roundId = 1L;
            Long groupId = 10L;

            Round round = createRound(roundId, 1L);
            RoundGroup group = createRoundGroup(groupId, roundId, null, "빈그룹", 0);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(orderRepository.findByRoundIdAndGroupIdIn(roundId, List.of(groupId)))
                    .willReturn(List.of());
            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));

            // when
            NodeOrdersResponse response = settlementService.getGroupOrders(roundId, groupId);

            // then
            assertThat(response.getGroupId()).isEqualTo(groupId);
            assertThat(response.getGroupName()).isEqualTo("빈그룹");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("하위 그룹 조회 시 상위 그룹 주문도 포함된다")
        void getGroupOrders_includesAncestorOrders() {
            // given
            Long roundId = 1L;
            Long parentGroupId = 10L;
            Long childGroupId = 20L;

            Round round = createRound(roundId, 1L);
            RoundGroup parentGroup = createRoundGroup(parentGroupId, roundId, null, "부모", 0);
            RoundGroup childGroup = createRoundGroup(childGroupId, roundId, parentGroupId, "자식", 1);

            Order parentOrder = createOrderWithGroup(1L, roundId, parentGroupId, "부모",
                    1L, "삼겹살", 1, new BigDecimal("30000"), "메인");
            Order childOrder = createOrderWithGroup(2L, roundId, childGroupId, "자식",
                    2L, "맥주", 1, new BigDecimal("5000"), "주류");

            OrderDetail detail1 = createOrderDetail(1L, 1L, 1L);
            OrderDetail detail2 = createOrderDetail(2L, 2L, 2L);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(parentGroup, childGroup));
            given(orderRepository.findByRoundIdAndGroupIdIn(roundId, List.of(childGroupId, parentGroupId)))
                    .willReturn(List.of(parentOrder, childOrder));
            given(orderDetailRepository.findByOrderIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(detail1, detail2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            NodeOrdersResponse response = settlementService.getGroupOrders(roundId, childGroupId);

            // then — 상위+자식 주문 모두 포함
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("35000"));
            assertThat(response.getCategories()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("멤버별 정산 나머지 할당 테스트")
    class CalculateMemberSettlementRemainderTest {

        /**
         * 10,000원을 3명이 균등 분배하면 몫은 3,333원이고 나머지는 1원이다.
         * 나머지는 첫 번째 멤버(index 0)에게 할당되므로
         * 첫 번째 멤버 = 3,334원, 나머지 멤버 = 각 3,333원이 되어
         * 분배 합계(3,334 + 3,333 + 3,333 = 10,000)가 원금과 동일해야 한다.
         */
        @Test
        @DisplayName("10,000원 3명 분배 시 나머지 1원이 첫 번째 멤버에 할당되어 합계 == 원금")
        void calculateMemberSettlement_remainder_assignedToFirstMember() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            // 10,000원 주문: 3명이 균등 분배 → 3,333원씩, 나머지 1원
            Order order = createOrderWithGroup(1L, roundId, groupId, "전체",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);
            RoundParticipant p3 = createParticipant(3L, roundId, 3L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "전체", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of(order));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2, p3));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(anyList()))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L),
                            createRoundGroupMember(3L, groupId, 3L)));
            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, 1L))
                    .willReturn(Optional.empty());

            // when — 첫 번째 멤버(1L) 정산
            MemberSettlementResponse responseMember1 = settlementService
                    .calculateMemberSettlement(sessionId, 1L, RemainderStrategyType.OWNER);

            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, 2L))
                    .willReturn(Optional.empty());
            MemberSettlementResponse responseMember2 = settlementService
                    .calculateMemberSettlement(sessionId, 2L, RemainderStrategyType.OWNER);

            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, 3L))
                    .willReturn(Optional.empty());
            MemberSettlementResponse responseMember3 = settlementService
                    .calculateMemberSettlement(sessionId, 3L, RemainderStrategyType.OWNER);

            // then — 나머지 1원이 첫 번째 멤버에 할당됨
            assertThat(responseMember1.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("3334")); // 3,333 + 1(나머지)
            assertThat(responseMember2.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("3333"));
            assertThat(responseMember3.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("3333"));

            // then — 분배 합계 == 원금(10,000원)
            BigDecimal distributionSum = responseMember1.getTotalAmount()
                    .add(responseMember2.getTotalAmount())
                    .add(responseMember3.getTotalAmount());
            assertThat(distributionSum).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("나머지 없는 균등 분배 시 합계 == 원금")
        void calculateMemberSettlement_noRemainder_sumEqualsTotal() {
            // given
            Long sessionId = 1L;
            Long roundId = 1L;
            Long groupId = 10L;

            Session session = createSession(sessionId, 1L);
            Round round = createRound(roundId, sessionId);

            // 10,000원 주문: 2명이 균등 분배 → 5,000원씩, 나머지 0원
            Order order = createOrderWithGroup(1L, roundId, groupId, "전체",
                    1L, "메뉴1", 1, new BigDecimal("10000"), null);

            RoundParticipant p1 = createParticipant(1L, roundId, 1L);
            RoundParticipant p2 = createParticipant(2L, roundId, 2L);

            RoundGroup group = createRoundGroup(groupId, roundId, null, "전체", 0);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round));
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of(order));
            given(roundParticipantRepository.findByRoundId(roundId))
                    .willReturn(List.of(p1, p2));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(anyList()))
                    .willReturn(List.of(
                            createRoundGroupMember(1L, groupId, 1L),
                            createRoundGroupMember(2L, groupId, 2L)));
            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, 1L))
                    .willReturn(Optional.empty());
            given(settlementPaymentRepository
                    .findBySessionIdAndOrgMemberId(sessionId, 2L))
                    .willReturn(Optional.empty());

            // when
            MemberSettlementResponse responseMember1 = settlementService
                    .calculateMemberSettlement(sessionId, 1L, RemainderStrategyType.OWNER);
            MemberSettlementResponse responseMember2 = settlementService
                    .calculateMemberSettlement(sessionId, 2L, RemainderStrategyType.OWNER);

            // then — 각 5,000원, 합계 == 10,000원
            assertThat(responseMember1.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(responseMember2.getTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("5000"));

            BigDecimal distributionSum = responseMember1.getTotalAmount()
                    .add(responseMember2.getTotalAmount());
            assertThat(distributionSum).isEqualByComparingTo(new BigDecimal("10000"));
        }
    }

    // === Helper Methods ===

    private Session createSession(Long id, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private Round createRound(Long id, Long sessionId) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title("테스트 라운드")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private RoundParticipant createParticipant(Long id, Long roundId, Long orgMemberId) {
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(roundId)
                .orgMemberId(orgMemberId)
                .build();
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    private Order createOrder(Long id, Long roundId, Long menuId,
                               int quantity, BigDecimal totalPrice, String menuCategory) {
        Order order = Order.builder()
                .roundId(roundId)
                .menuId(menuId)
                .menuName("메뉴" + menuId)
                .menuPrice(quantity > 0 ? totalPrice.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .menuCategory(menuCategory)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private Order createOrder(Long id, Long roundId, Long menuId,
                               int quantity, BigDecimal totalPrice) {
        return createOrder(id, roundId, menuId, quantity, totalPrice, null);
    }

    private OrderDetail createOrderDetail(Long id, Long orderId, Long orgMemberId) {
        OrderDetail detail = OrderDetail.builder()
                .orderId(orderId)
                .orgMemberId(orgMemberId)
                .build();
        ReflectionTestUtils.setField(detail, "id", id);
        return detail;
    }

    private OrderDetail createOrderDetailWithRatio(Long id, Long orderId,
                                                    Long orgMemberId, int shareRatio) {
        OrderDetail detail = OrderDetail.builder()
                .orderId(orderId)
                .orgMemberId(orgMemberId)
                .shareRatio(shareRatio)
                .build();
        ReflectionTestUtils.setField(detail, "id", id);
        return detail;
    }

    private Order createOrderWithGroup(Long id, Long roundId, Long groupId, String groupName,
                                        Long menuId, String menuName, int quantity,
                                        BigDecimal totalPrice, String menuCategory) {
        Order order = Order.builder()
                .roundId(roundId)
                .groupId(groupId)
                .groupName(groupName)
                .menuId(menuId)
                .menuName(menuName)
                .menuPrice(quantity > 0 ? totalPrice.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .menuCategory(menuCategory)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private RoundGroup createRoundGroup(Long id, Long roundId, Long parentGroupId,
                                         String name, int depth) {
        RoundGroup group = RoundGroup.builder()
                .roundId(roundId)
                .parentGroupId(parentGroupId)
                .name(name)
                .depth(depth)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private RoundGroupMember createRoundGroupMember(Long id, Long groupId, Long orgMemberId) {
        RoundGroupMember member = RoundGroupMember.builder()
                .groupId(groupId)
                .orgMemberId(orgMemberId)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
