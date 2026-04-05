package com.dirtypay.domain.store.entity;

import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link StoreOrder} Entity 단위 테스트.
 *
 * <p>주문 생성, 상태 전이(confirm/complete/cancel), verifyModifiable 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class StoreOrderTest {

    @Nested
    @DisplayName("StoreOrder 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Builder로 StoreOrder 생성 시 초기 상태는 PENDING이고 모든 필드가 설정된다")
        void StoreOrder_create_allFieldsSetWithUuid() {
            // given
            String orderNumber = UUID.randomUUID().toString();
            BigDecimal totalPrice = new BigDecimal("30000");

            // when
            StoreOrder order = StoreOrder.builder()
                    .storeId(1L)
                    .menuId(10L)
                    .quantity(2)
                    .totalPrice(totalPrice)
                    .orderNumber(orderNumber)
                    .customerName("홍길동")
                    .customerPhone("010-1234-5678")
                    .build();

            // then
            assertThat(order.getStoreId()).isEqualTo(1L);
            assertThat(order.getMenuId()).isEqualTo(10L);
            assertThat(order.getQuantity()).isEqualTo(2);
            assertThat(order.getTotalPrice()).isEqualByComparingTo(totalPrice);
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.PENDING);  // 초기 상태 PENDING
            assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
            assertThat(order.getCustomerName()).isEqualTo("홍길동");
            assertThat(order.getCustomerPhone()).isEqualTo("010-1234-5678");
        }
    }

    @Nested
    @DisplayName("StoreOrder 확인 처리 테스트")
    class ConfirmTest {

        @Test
        @DisplayName("PENDING 상태에서 confirm 호출 시 CONFIRMED 상태로 전환된다")
        void StoreOrder_confirm_success() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.PENDING);

            // when
            order.confirm();

            // then
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("StoreOrder 완료 처리 테스트")
    class CompleteTest {

        @Test
        @DisplayName("CONFIRMED 상태에서 complete 호출 시 COMPLETED 상태로 전환된다")
        void StoreOrder_complete_success() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            order.confirm();  // PENDING -> CONFIRMED
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CONFIRMED);

            // when
            order.complete();

            // then
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("PENDING 상태에서 complete 호출 시 BusinessException이 발생한다")
        void StoreOrder_complete_failWhenNotConfirmed() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.PENDING);

            // when & then
            assertThatThrownBy(order::complete)
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("StoreOrder 취소 처리 테스트")
    class CancelTest {

        @Test
        @DisplayName("PENDING 상태에서 cancel 호출 시 CANCELLED 상태로 전환된다")
        void StoreOrder_cancel_success() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.PENDING);

            // when
            order.cancel();

            // then
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("StoreOrder 변경 가능 상태 검증 테스트")
    class VerifyModifiableTest {

        @Test
        @DisplayName("COMPLETED 상태에서 verifyModifiable 호출 시 BusinessException이 발생한다")
        void StoreOrder_verifyModifiable_throwsWhenAlreadyCompleted() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            order.confirm();
            order.complete();
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);

            // when & then
            assertThatThrownBy(order::verifyModifiable)
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("CANCELLED 상태에서 confirm 호출 시 BusinessException이 발생한다 (역전이 방지)")
        void StoreOrder_changeStatus_completedToPending_throwsException() {
            // given
            StoreOrder order = createOrder(1L, 1L, 10L, 2, new BigDecimal("30000"));
            order.cancel();
            assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CANCELLED);

            // when & then — CANCELLED 상태에서 confirm 시도는 verifyModifiable에서 차단됨
            assertThatThrownBy(order::confirm)
                    .isInstanceOf(BusinessException.class);
        }
    }

    // === Helper Methods ===

    private StoreOrder createOrder(Long id, Long storeId, Long menuId, int quantity, BigDecimal totalPrice) {
        StoreOrder order = StoreOrder.builder()
                .storeId(storeId)
                .menuId(menuId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .orderNumber(UUID.randomUUID().toString())
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }
}
