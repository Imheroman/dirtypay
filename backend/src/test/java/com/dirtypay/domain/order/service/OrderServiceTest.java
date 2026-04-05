package com.dirtypay.domain.order.service;

import com.dirtypay.domain.order.dto.request.OrderCreateRequest;
import com.dirtypay.domain.order.dto.request.OrderUpdateRequest;
import com.dirtypay.domain.order.dto.response.OrderResponse;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.global.common.enums.ErrorCode;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundGroupRepository roundGroupRepository;

    @Mock
    private RoundGroupMemberRepository roundGroupMemberRepository;

    @Mock
    private OrgMemberService orgMemberService;

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("주문 생성 성공 — totalPrice가 올바르게 계산된다")
        void createOrder_success() {
            // given
            Long roundId = 1L;
            Long sessionId = 1L;

            OrderCreateRequest request = new OrderCreateRequest();
            ReflectionTestUtils.setField(request, "menuId", 1L);
            ReflectionTestUtils.setField(request, "groupId", 10L);
            ReflectionTestUtils.setField(request, "quantity", 3);
            ReflectionTestUtils.setField(request, "memberIds", List.of(1L, 2L));

            Round round = createRound(roundId, sessionId);
            RoundGroup group = createGroup(10L, roundId, "1팀");
            StoreMenu storeMenu = createStoreMenu(1L, "김치찌개", new BigDecimal("9000"));

            Order savedOrder = Order.builder()
                    .roundId(roundId)
                    .menuId(1L)
                    .menuName("김치찌개")
                    .menuPrice(new BigDecimal("9000"))
                    .quantity(3)
                    .totalPrice(new BigDecimal("27000"))
                    .groupId(10L)
                    .groupName("1팀")
                    .build();
            ReflectionTestUtils.setField(savedOrder, "id", 1L);

            OrderDetail detail1 = createOrderDetail(1L, 1L, 1L);
            OrderDetail detail2 = createOrderDetail(2L, 1L, 2L);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            RoundGroupMember member1 = RoundGroupMember.builder()
                    .groupId(10L).orgMemberId(1L).build();
            RoundGroupMember member2 = RoundGroupMember.builder()
                    .groupId(10L).orgMemberId(2L).build();

            given(roundGroupRepository.findById(10L))
                    .willReturn(Optional.of(group));
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(10L)))
                    .willReturn(List.of(member1, member2));
            given(storeMenuRepository.findById(1L))
                    .willReturn(Optional.of(storeMenu));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(orderDetailRepository.saveAll(anyList()))
                    .willReturn(List.of(detail1, detail2));
            given(orgMemberService.getNicknameMap(anyList()))
                    .willReturn(java.util.Map.of(1L, "철수", 2L, "영희"));

            // when
            OrderResponse response = orderService.createOrder(roundId, request);

            // then
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("27000"));
            assertThat(response.getDetails()).hasSize(2);
        }

        @Test
        @DisplayName("Group의 roundId가 Round ID와 불일치하면 예외 발생")
        void createOrder_groupRoundMismatch_throwsException() {
            // given
            Long roundId = 1L;

            OrderCreateRequest request = new OrderCreateRequest();
            ReflectionTestUtils.setField(request, "menuId", 1L);
            ReflectionTestUtils.setField(request, "groupId", 10L);
            ReflectionTestUtils.setField(request, "quantity", 1);
            ReflectionTestUtils.setField(request, "memberIds", List.of(1L));

            Round round = createRound(roundId, 1L);
            RoundGroup groupFromAnotherRound = createGroup(10L, 99L, "다른팀"); // roundId=99, not 1

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(roundGroupRepository.findById(10L))
                    .willReturn(Optional.of(groupFromAnotherRound));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(roundId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_GROUP_ROUND_MISMATCH);
        }

        @Test
        @DisplayName("CLOSED 상태에서 주문 생성 시 예외 발생")
        void createOrder_roundClosed() {
            // given
            Long roundId = 1L;
            Round round = createRound(roundId, 1L);
            round.changeStatus(RoundStatus.CLOSED);

            OrderCreateRequest request = new OrderCreateRequest();
            ReflectionTestUtils.setField(request, "menuId", 1L);
            ReflectionTestUtils.setField(request, "groupId", 10L);
            ReflectionTestUtils.setField(request, "quantity", 1);
            ReflectionTestUtils.setField(request, "memberIds", List.of(1L));

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(roundId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 수정 테스트")
    class UpdateOrderTest {

        @Test
        @DisplayName("주문 수량 변경 시 totalPrice가 재계산된다")
        void updateOrder_recalculatesTotalPrice() {
            // given
            Long orderId = 1L;

            OrderUpdateRequest request = new OrderUpdateRequest();
            ReflectionTestUtils.setField(request, "quantity", 5);

            Order order = Order.builder()
                    .roundId(1L)
                    .menuId(1L)
                    .menuName("김치찌개")
                    .menuPrice(new BigDecimal("9000"))
                    .quantity(3)
                    .totalPrice(new BigDecimal("27000"))
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            Round round = createRound(1L, 1L);

            given(orderRepository.findById(orderId))
                    .willReturn(Optional.of(order));
            given(roundRepository.findById(order.getRoundId()))
                    .willReturn(Optional.of(round));
            given(orderDetailRepository.findByOrderId(orderId))
                    .willReturn(List.of());
            given(orgMemberService.getNicknameMap(List.of()))
                    .willReturn(java.util.Map.of());

            // when
            OrderResponse response = orderService.updateOrder(orderId, request);

            // then
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("45000"));
            assertThat(response.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 주문 수정 시 EntityNotFoundException 발생")
        void updateOrder_notFound_throwsException() {
            // given
            Long orderId = 999L;
            OrderUpdateRequest request = new OrderUpdateRequest();
            ReflectionTestUtils.setField(request, "quantity", 5);

            given(orderRepository.findById(orderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.updateOrder(orderId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CLOSED 라운드의 주문 수정 불가")
        void updateOrder_closedRound_throwsException() {
            // given
            Long orderId = 1L;
            OrderUpdateRequest request = new OrderUpdateRequest();
            ReflectionTestUtils.setField(request, "quantity", 5);

            Order order = Order.builder()
                    .roundId(1L)
                    .menuId(1L)
                    .menuName("김치찌개")
                    .menuPrice(new BigDecimal("9000"))
                    .quantity(3)
                    .totalPrice(new BigDecimal("27000"))
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            Round closedRound = createRound(1L, 1L);
            ReflectionTestUtils.setField(closedRound, "status", RoundStatus.CLOSED);

            given(orderRepository.findById(orderId))
                    .willReturn(Optional.of(order));
            given(roundRepository.findById(order.getRoundId()))
                    .willReturn(Optional.of(closedRound));

            // when & then
            assertThatThrownBy(() -> orderService.updateOrder(orderId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class GetOrdersTest {

        @Test
        @DisplayName("라운드의 주문 목록 조회 성공")
        void getOrdersByRound_success() {
            // given
            Long roundId = 1L;
            Order order = Order.builder()
                    .roundId(roundId)
                    .menuId(1L)
                    .menuName("김치찌개")
                    .menuPrice(new BigDecimal("9000"))
                    .quantity(2)
                    .totalPrice(new BigDecimal("18000"))
                    .build();
            ReflectionTestUtils.setField(order, "id", 1L);

            OrderDetail detail = createOrderDetail(1L, 1L, 1L);
            given(orderRepository.findByRoundId(roundId))
                    .willReturn(List.of(order));
            given(orderDetailRepository.findByOrderIdIn(List.of(1L)))
                    .willReturn(List.of(detail));
            given(orgMemberService.getNicknameMap(List.of(1L)))
                    .willReturn(java.util.Map.of(1L, "철수"));

            // when
            List<OrderResponse> responses = orderService.getOrdersByRound(roundId, null, null);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getMenuName()).isEqualTo("김치찌개");
        }
    }

    // === Helper Methods ===

    private RoundGroup createGroup(Long id, Long roundId, String name) {
        RoundGroup group = RoundGroup.builder()
                .roundId(roundId)
                .name(name)
                .depth(0)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
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

    private StoreMenu createStoreMenu(Long id, String name, BigDecimal price) {
        StoreMenu storeMenu = StoreMenu.builder()
                .storeId(1L)
                .name(name)
                .price(price)
                .available(true)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(storeMenu, "id", id);
        return storeMenu;
    }

    private OrderDetail createOrderDetail(Long id, Long orderId, Long orgMemberId) {
        OrderDetail detail = OrderDetail.builder()
                .orderId(orderId)
                .orgMemberId(orgMemberId)
                .build();
        ReflectionTestUtils.setField(detail, "id", id);
        return detail;
    }

}
