package com.dirtypay.domain.round.service;

import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.dto.request.RoundCreateRequest;
import com.dirtypay.domain.round.dto.request.RoundStatusChangeRequest;
import com.dirtypay.domain.round.dto.request.RoundUpdateRequest;
import com.dirtypay.domain.round.dto.response.RoundParticipantResponse;
import com.dirtypay.domain.round.dto.response.RoundResponse;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

    @InjectMocks
    private RoundService roundService;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundParticipantRepository roundParticipantRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private OrgMemberService orgMemberService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Nested
    @DisplayName("라운드 생성 테스트")
    class CreateRoundTest {

        @Test
        @DisplayName("라운드 생성 성공 — 참여자가 초기화된다")
        void createRound_success() {
            // given
            Long sessionId = 1L;

            Long storeId = 10L;

            RoundCreateRequest request = new RoundCreateRequest();
            ReflectionTestUtils.setField(request, "title", "점심 식사");
            ReflectionTestUtils.setField(request, "place", "강남역");
            ReflectionTestUtils.setField(request, "roundDate", LocalDate.of(2026, 2, 17));
            ReflectionTestUtils.setField(request, "sortOrder", 1);
            ReflectionTestUtils.setField(request, "storeId", storeId);

            Round savedRound = createRoundWithStore(1L, sessionId, "점심 식사", storeId);
            Session session = createSession(sessionId, SessionStatus.ACTIVE);

            given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(roundRepository.save(any(Round.class))).willReturn(savedRound);

            OrgMember member1 = createOrgMember(1L, "철수");
            OrgMember member2 = createOrgMember(2L, "영희");
            given(orgMemberRepository.findBySessionId(sessionId))
                    .willReturn(List.of(member1, member2));
            given(roundParticipantRepository.saveAll(anyList()))
                    .willReturn(List.of());

            // when
            RoundResponse response = roundService.createRound(sessionId, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("점심 식사");
            assertThat(response.getStatus()).isEqualTo(RoundStatus.OPEN);
            verify(roundParticipantRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("라운드 조회 테스트")
    class GetRoundTest {

        @Test
        @DisplayName("라운드 목록 조회 성공")
        void getRounds_success() {
            // given
            Long sessionId = 1L;
            Round round1 = createRound(1L, sessionId, "점심");
            Round round2 = createRound(2L, sessionId, "저녁");

            given(roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId))
                    .willReturn(List.of(round1, round2));
            given(orderRepository.sumTotalPriceByRoundId(anyLong()))
                    .willReturn(BigDecimal.ZERO);
            given(roundParticipantRepository.countByRoundId(anyLong()))
                    .willReturn(0L);

            // when
            List<RoundResponse> responses = roundService.getRounds(sessionId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getTitle()).isEqualTo("점심");
            assertThat(responses.get(1).getTitle()).isEqualTo("저녁");
        }

        @Test
        @DisplayName("존재하지 않는 라운드 조회 시 예외 발생")
        void getRound_notFound() {
            // given
            given(roundRepository.findById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roundService.getRound(999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("라운드 상태 변경 테스트")
    class ChangeStatusTest {

        @Test
        @DisplayName("라운드 상태를 CLOSED로 변경 성공")
        void changeStatus_success() {
            // given
            Long roundId = 1L;
            Long sessionId = 1L;
            Round round = createRound(roundId, sessionId, "점심");
            Session session = createSession(sessionId, SessionStatus.ACTIVE);

            RoundStatusChangeRequest request = new RoundStatusChangeRequest();
            ReflectionTestUtils.setField(request, "status", RoundStatus.CLOSED);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            RoundResponse response = roundService.changeStatus(roundId, request);

            // then
            assertThat(response.getStatus()).isEqualTo(RoundStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("참여자 제외/포함 테스트")
    class ParticipantExcludeIncludeTest {

        @Test
        @DisplayName("참여자 제외 성공")
        void excludeParticipant_success() {
            // given
            Long roundId = 1L;
            Long participantId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            RoundParticipant participant = createParticipant(participantId, roundId, 1L);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(roundParticipantRepository.findById(participantId))
                    .willReturn(Optional.of(participant));
            given(orgMemberRepository.findById(participant.getOrgMemberId()))
                    .willReturn(Optional.of(createOrgMember(1L, "철수")));

            // when
            RoundParticipantResponse response = roundService.excludeParticipant(roundId, participantId);

            // then
            assertThat(response.isExcluded()).isTrue();
        }

        @Test
        @DisplayName("CLOSED 상태에서 참여자 제외 시 예외 발생")
        void excludeParticipant_roundClosed() {
            // given
            Long roundId = 1L;
            Long participantId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            round.changeStatus(RoundStatus.CLOSED);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));

            // when & then
            assertThatThrownBy(() -> roundService.excludeParticipant(roundId, participantId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("참여자 포함 성공")
        void includeParticipant_success() {
            // given
            Long roundId = 1L;
            Long participantId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            RoundParticipant participant = createParticipant(participantId, roundId, 1L);
            participant.exclude();

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(roundParticipantRepository.findById(participantId))
                    .willReturn(Optional.of(participant));
            given(orgMemberRepository.findById(participant.getOrgMemberId()))
                    .willReturn(Optional.of(createOrgMember(1L, "철수")));

            // when
            RoundParticipantResponse response = roundService.includeParticipant(roundId, participantId);

            // then
            assertThat(response.isExcluded()).isFalse();
        }
    }

    @Nested
    @DisplayName("완료 상태 수정 방지 테스트")
    class ClosedStateGuardTest {

        @Test
        @DisplayName("ARCHIVED 세션에서 라운드 생성 시 예외 발생")
        void createRound_archivedSession_throwsException() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, SessionStatus.ARCHIVED);

            RoundCreateRequest request = new RoundCreateRequest();
            ReflectionTestUtils.setField(request, "title", "점심");
            ReflectionTestUtils.setField(request, "sortOrder", 1);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> roundService.createRound(sessionId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CLOSED 라운드 수정 시 예외 발생 (title 변경)")
        void updateRound_closed_throwsException() {
            // given
            Long roundId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            round.changeStatus(RoundStatus.CLOSED);

            RoundUpdateRequest request = new RoundUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "저녁");
            ReflectionTestUtils.setField(request, "place", round.getPlace());
            ReflectionTestUtils.setField(request, "roundDate", round.getRoundDate());
            ReflectionTestUtils.setField(request, "sortOrder", round.getSortOrder());
            ReflectionTestUtils.setField(request, "storeId", round.getStoreId());

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));

            // when & then
            assertThatThrownBy(() -> roundService.updateRound(roundId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CLOSED 라운드에서 sortOrder만 변경은 허용된다")
        void updateRound_closed_sortOrderOnly_success() {
            // given
            Long roundId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            round.changeStatus(RoundStatus.CLOSED);

            RoundUpdateRequest request = new RoundUpdateRequest();
            ReflectionTestUtils.setField(request, "title", round.getTitle());
            ReflectionTestUtils.setField(request, "place", round.getPlace());
            ReflectionTestUtils.setField(request, "roundDate", round.getRoundDate());
            ReflectionTestUtils.setField(request, "sortOrder", 99);
            ReflectionTestUtils.setField(request, "storeId", round.getStoreId());

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));

            // when
            RoundResponse response = roundService.updateRound(roundId, request);

            // then
            assertThat(response.getSortOrder()).isEqualTo(99);
        }

        @Test
        @DisplayName("CLOSED 라운드도 삭제 성공")
        void deleteRound_closed_success() {
            // given
            Long roundId = 1L;
            Round round = createRound(roundId, 1L, "점심");
            round.changeStatus(RoundStatus.CLOSED);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));

            // when
            roundService.deleteRound(roundId);

            // then
            assertThat(round.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("ARCHIVED 세션의 라운드 상태 변경 시 예외 발생")
        void changeStatus_archivedSession_throwsException() {
            // given
            Long roundId = 1L;
            Long sessionId = 1L;
            Round round = createRound(roundId, sessionId, "점심");
            round.changeStatus(RoundStatus.CLOSED);
            Session session = createSession(sessionId, SessionStatus.ARCHIVED);

            RoundStatusChangeRequest request = new RoundStatusChangeRequest();
            ReflectionTestUtils.setField(request, "status", RoundStatus.OPEN);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> roundService.changeStatus(roundId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("라운드 가게 변경 테스트")
    class UpdateRoundStoreChangeTest {

        @Test
        @DisplayName("가게 변경 성공")
        void updateRound_storeChange_success() {
            // given
            Long roundId = 1L;
            Long oldStoreId = 10L;
            Long newStoreId = 20L;
            Round round = createRoundWithStore(roundId, 1L, "점심", oldStoreId);

            RoundUpdateRequest request = new RoundUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "점심");
            ReflectionTestUtils.setField(request, "sortOrder", 1);
            ReflectionTestUtils.setField(request, "storeId", newStoreId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orderRepository.existsByRoundId(roundId)).willReturn(false);

            // when
            RoundResponse response = roundService.updateRound(roundId, request);

            // then
            assertThat(response.getStoreId()).isEqualTo(newStoreId);
        }

        @Test
        @DisplayName("주문 존재 시 가게 변경 거부")
        void updateRound_storeChange_hasOrders_failure() {
            // given
            Long roundId = 1L;
            Long oldStoreId = 10L;
            Long newStoreId = 20L;
            Round round = createRoundWithStore(roundId, 1L, "점심", oldStoreId);

            RoundUpdateRequest request = new RoundUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "점심");
            ReflectionTestUtils.setField(request, "sortOrder", 1);
            ReflectionTestUtils.setField(request, "storeId", newStoreId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orderRepository.existsByRoundId(roundId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> roundService.updateRound(roundId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_HAS_ORDERS);
        }

        @Test
        @DisplayName("같은 가게면 주문 검증 미실행")
        void updateRound_sameStore_noMenuChange() {
            // given
            Long roundId = 1L;
            Long storeId = 10L;
            Round round = createRoundWithStore(roundId, 1L, "점심", storeId);

            RoundUpdateRequest request = new RoundUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "저녁으로 변경");
            ReflectionTestUtils.setField(request, "sortOrder", 1);
            ReflectionTestUtils.setField(request, "storeId", storeId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));

            // when
            RoundResponse response = roundService.updateRound(roundId, request);

            // then
            assertThat(response.getTitle()).isEqualTo("저녁으로 변경");
        }
    }

    // === Helper Methods ===

    private Session createSession(Long id, SessionStatus status) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(1L)
                .status(status)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private Round createRound(Long id, Long sessionId, String title) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title(title)
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private Round createRoundWithStore(Long id, Long sessionId, String title, Long storeId) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title(title)
                .sortOrder(1)
                .storeId(storeId)
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

    private OrgMember createOrgMember(Long id, String nickname) {
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
