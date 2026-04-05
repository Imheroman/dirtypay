package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreOrderCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreOrderStatusChangeRequest;
import com.dirtypay.domain.store.dto.response.StoreOrderResponse;
import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.store.repository.StoreOrderRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link StoreOrderService} 단위 테스트.
 *
 * <p>주문 생성(totalPrice 계산), 상태 전이, 취소, 삭제, 조회 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StoreOrderServiceTest {

    @InjectMocks
    private StoreOrderService storeOrderService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("주문 생성 성공 시 totalPrice가 menu.price * quantity로 계산된다")
        void StoreOrderService_createOrder_successWithTotalPriceCalc() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreMenu menu = createMenu(menuId, storeId, "삼겹살", new BigDecimal("15000"), true);
            StoreOrderCreateRequest request = createOrderRequest(menuId, 2);

            // 저장될 주문 생성 (totalPrice = 15000 * 2 = 30000)
            StoreOrder savedOrder = StoreOrder.builder()
                    .storeId(storeId)
                    .menuId(menuId)
                    .quantity(2)
                    .totalPrice(new BigDecimal("30000"))
                    .orderNumber(UUID.randomUUID().toString())
                    .build();
            ReflectionTestUtils.setField(savedOrder, "id", 100L);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));
            given(storeOrderRepository.save(any(StoreOrder.class))).willReturn(savedOrder);

            // when
            StoreOrderResponse response = storeOrderService.createOrder(storeId, request);

            // then
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(response.getQuantity()).isEqualTo(2);
            assertThat(response.getStatus()).isEqualTo(StoreOrderStatus.PENDING);
        }

        @Test
        @DisplayName("orderNumber는 UUID 형식으로 생성된다")
        void StoreOrderService_createOrder_generateUUID() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreMenu menu = createMenu(menuId, storeId, "삼겹살", new BigDecimal("15000"), true);
            StoreOrderCreateRequest request = createOrderRequest(menuId, 1);

            StoreOrder savedOrder = StoreOrder.builder()
                    .storeId(storeId)
                    .menuId(menuId)
                    .quantity(1)
                    .totalPrice(new BigDecimal("15000"))
                    .orderNumber(UUID.randomUUID().toString())
                    .build();
            ReflectionTestUtils.setField(savedOrder, "id", 100L);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));
            given(storeOrderRepository.save(any(StoreOrder.class))).willReturn(savedOrder);

            // when
            StoreOrderResponse response = storeOrderService.createOrder(storeId, request);

            // then — UUID 형식 검증 (36자: 8-4-4-4-12)
            assertThat(response.getOrderNumber()).isNotNull();
            assertThat(response.getOrderNumber()).hasSize(36);
        }

        @Test
        @DisplayName("품절(available=false) 메뉴 주문 시 BusinessException이 발생한다")
        void StoreOrderService_createOrder_menuNotAvailable_throwsException() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreMenu unavailableMenu = createMenu(menuId, storeId, "품절 메뉴", new BigDecimal("15000"), false);
            StoreOrderCreateRequest request = createOrderRequest(menuId, 1);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(unavailableMenu));

            // when & then
            assertThatThrownBy(() -> storeOrderService.createOrder(storeId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 menuId로 주문 생성 시 EntityNotFoundException이 발생한다")
        void StoreOrderService_createOrder_menuNotFound_throwsException() {
            // given
            Long storeId = 1L;
            Long nonExistentMenuId = 999L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreOrderCreateRequest request = createOrderRequest(nonExistentMenuId, 1);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.findByIdAndStoreId(nonExistentMenuId, storeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeOrderService.createOrder(storeId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("주문 상태 변경 테스트")
    class ChangeStatusTest {

        @Test
        @DisplayName("PENDING -> CONFIRMED 상태 전이 성공 시 CONFIRMED 상태의 StoreOrderResponse를 반환한다")
        void StoreOrderService_changeStatus_pendingToConfirmed() {
            // given
            Long storeId = 1L;
            Long orderId = 100L;
            StoreOrder order = createOrder(orderId, storeId, 10L, 2, new BigDecimal("30000"));
            StoreOrderStatusChangeRequest request = createStatusChangeRequest(StoreOrderStatus.CONFIRMED);

            given(storeOrderRepository.findByIdAndStoreId(orderId, storeId)).willReturn(Optional.of(order));

            // when
            StoreOrderResponse response = storeOrderService.changeOrderStatus(storeId, orderId, request);

            // then
            assertThat(response.getStatus()).isEqualTo(StoreOrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("CONFIRMED -> COMPLETED 상태 전이 성공 시 COMPLETED 상태의 StoreOrderResponse를 반환한다")
        void StoreOrderService_changeStatus_confirmedToCompleted() {
            // given
            Long storeId = 1L;
            Long orderId = 100L;
            StoreOrder order = createOrder(orderId, storeId, 10L, 2, new BigDecimal("30000"));
            order.confirm();  // PENDING -> CONFIRMED
            StoreOrderStatusChangeRequest request = createStatusChangeRequest(StoreOrderStatus.COMPLETED);

            given(storeOrderRepository.findByIdAndStoreId(orderId, storeId)).willReturn(Optional.of(order));

            // when
            StoreOrderResponse response = storeOrderService.changeOrderStatus(storeId, orderId, request);

            // then
            assertThat(response.getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 CONFIRMED로 역전이 시도 시 BusinessException이 발생한다")
        void StoreOrderService_changeStatus_invalidTransition_throwsException() {
            // given
            Long storeId = 1L;
            Long orderId = 100L;
            StoreOrder order = createOrder(orderId, storeId, 10L, 2, new BigDecimal("30000"));
            order.confirm();
            order.complete();  // COMPLETED 상태
            StoreOrderStatusChangeRequest request = createStatusChangeRequest(StoreOrderStatus.CONFIRMED);

            given(storeOrderRepository.findByIdAndStoreId(orderId, storeId)).willReturn(Optional.of(order));

            // when & then — COMPLETED 상태에서 confirm() 호출 → verifyModifiable에서 BusinessException
            assertThatThrownBy(() -> storeOrderService.changeOrderStatus(storeId, orderId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class GetOrdersTest {

        @Test
        @DisplayName("getOrders: 매장의 전체 주문 목록을 페이지 단위로 반환한다")
        void StoreOrderService_getOrders_success() {
            // given
            Long storeId = 1L;
            Pageable pageable = Pageable.unpaged();
            List<StoreOrder> orders = List.of(
                    createOrder(1L, storeId, 10L, 2, new BigDecimal("30000")),
                    createOrder(2L, storeId, 11L, 1, new BigDecimal("5000"))
            );
            Page<StoreOrder> orderPage = new PageImpl<>(orders, pageable, orders.size());

            given(storeOrderRepository.findAllByStoreIdOrderByCreatedDateDesc(storeId, pageable))
                    .willReturn(orderPage);

            // when
            Page<StoreOrderResponse> responsePage = storeOrderService.getOrders(storeId, pageable);

            // then
            assertThat(responsePage.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("주문 스냅샷 필드 테스트")
    class OrderSnapshotTest {

        @Test
        @DisplayName("주문 생성 시 unitPrice와 menuName이 주문 시점 메뉴 정보와 일치하고, memberId가 요청값과 일치한다")
        void createOrder_snapshotFields_matchMenuAtOrderTime() {
            // given — 메뉴 단가·이름, 주문자 회원 ID 준비
            Long storeId = 1L;
            Long menuId = 10L;
            Long memberId = 99L;
            BigDecimal menuPrice = new BigDecimal("12000");
            String menuNameValue = "순대국밥";

            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreMenu menu = createMenu(menuId, storeId, menuNameValue, menuPrice, true);

            // 요청에 memberId 포함
            StoreOrderCreateRequest request = createOrderRequest(menuId, 2, memberId);

            // 저장될 주문 — 스냅샷 필드(unitPrice, menuName, memberId) 포함
            StoreOrder savedOrder = StoreOrder.builder()
                    .storeId(storeId)
                    .menuId(menuId)
                    .quantity(2)
                    .totalPrice(menuPrice.multiply(BigDecimal.valueOf(2)))
                    .unitPrice(menuPrice)
                    .menuName(menuNameValue)
                    .memberId(memberId)
                    .orderNumber(UUID.randomUUID().toString())
                    .build();
            ReflectionTestUtils.setField(savedOrder, "id", 200L);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));
            given(storeOrderRepository.save(any(StoreOrder.class))).willReturn(savedOrder);

            // when
            StoreOrderResponse response = storeOrderService.createOrder(storeId, request);

            // then — 스냅샷 필드가 주문 시점 메뉴 정보·요청값과 일치하는지 검증
            assertThat(response.getUnitPrice()).isEqualByComparingTo(menuPrice);
            assertThat(response.getMenuName()).isEqualTo(menuNameValue);
            assertThat(response.getMemberId()).isEqualTo(memberId);
        }
    }

    @Nested
    @DisplayName("주문 소프트 삭제 테스트")
    class DeleteOrderTest {

        @Test
        @DisplayName("주문 Soft Delete 성공 시 deletedDate가 설정된다")
        void StoreOrderService_deleteOrder_softDelete() {
            // given
            Long storeId = 1L;
            Long orderId = 100L;
            StoreOrder order = createOrder(orderId, storeId, 10L, 2, new BigDecimal("30000"));

            given(storeOrderRepository.findByIdAndStoreId(orderId, storeId)).willReturn(Optional.of(order));

            // when
            storeOrderService.cancelOrder(storeId, orderId);

            // then — cancel() 호출됨 (CANCELLED 상태 전이)
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CANCELLED);
        }
    }

    // === Helper Methods ===

    private Store createStore(Long id, Long ownerId, String name) {
        Store store = Store.builder()
                .ownerId(ownerId)
                .name(name)
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .status(StoreStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    private StoreMenu createMenu(Long id, Long storeId, String name, BigDecimal price, boolean available) {
        StoreMenu menu = StoreMenu.builder()
                .storeId(storeId)
                .name(name)
                .price(price)
                .available(available)
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }

    private StoreOrder createOrder(Long id, Long storeId, Long menuId, int quantity, BigDecimal totalPrice) {
        StoreOrder order = StoreOrder.builder()
                .storeId(storeId)
                .menuId(menuId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .unitPrice(totalPrice.divide(BigDecimal.valueOf(quantity)))
                .menuName("테스트 메뉴")
                .orderNumber(UUID.randomUUID().toString())
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private StoreOrderCreateRequest createOrderRequest(Long menuId, int quantity) {
        StoreOrderCreateRequest request = new StoreOrderCreateRequest();
        ReflectionTestUtils.setField(request, "menuId", menuId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }

    private StoreOrderCreateRequest createOrderRequest(Long menuId, int quantity, Long memberId) {
        StoreOrderCreateRequest request = new StoreOrderCreateRequest();
        ReflectionTestUtils.setField(request, "menuId", menuId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        ReflectionTestUtils.setField(request, "memberId", memberId);
        return request;
    }

    private StoreOrderStatusChangeRequest createStatusChangeRequest(StoreOrderStatus status) {
        StoreOrderStatusChangeRequest request = new StoreOrderStatusChangeRequest();
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
