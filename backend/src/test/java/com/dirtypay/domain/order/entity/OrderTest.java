package com.dirtypay.domain.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Order 생성 시 모든 필드가 올바르게 설정된다")
    void createOrder_allFieldsSet() {
        // given & when
        Order order = Order.builder()
                .roundId(1L)
                .menuId(10L)
                .menuName("테스트 메뉴")
                .menuPrice(BigDecimal.valueOf(15000))
                .quantity(2)
                .totalPrice(BigDecimal.valueOf(30000))
                .build();

        // then
        assertThat(order.getRoundId()).isEqualTo(1L);
        assertThat(order.getMenuId()).isEqualTo(10L);
        assertThat(order.getMenuName()).isEqualTo("테스트 메뉴");
        assertThat(order.getMenuPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(order.getQuantity()).isEqualTo(2);
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @Nested
    @DisplayName("updateQuantity 테스트")
    class UpdateQuantityTest {

        @Test
        @DisplayName("수량 변경 시 스냅샷 menuPrice 기반으로 totalPrice가 재계산된다")
        void updateQuantity_recalculatesTotalPrice() {
            // given
            Order order = Order.builder()
                    .roundId(1L)
                    .menuId(10L)
                    .menuName("테스트 메뉴")
                    .menuPrice(BigDecimal.valueOf(15000))
                    .quantity(2)
                    .totalPrice(BigDecimal.valueOf(30000))
                    .build();

            // when
            order.updateQuantity(3);

            // then
            assertThat(order.getQuantity()).isEqualTo(3);
            assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(45000));
        }

        @Test
        @DisplayName("수량 1로 변경 시 totalPrice는 메뉴 단가와 같다")
        void updateQuantity_toOne() {
            // given
            Order order = Order.builder()
                    .roundId(1L)
                    .menuId(10L)
                    .menuName("테스트 메뉴")
                    .menuPrice(BigDecimal.valueOf(15000))
                    .quantity(5)
                    .totalPrice(BigDecimal.valueOf(75000))
                    .build();

            // when
            order.updateQuantity(1);

            // then
            assertThat(order.getQuantity()).isEqualTo(1);
            assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        }

        @Test
        @DisplayName("수량 0일 때 totalPrice는 0이다")
        void updateQuantity_toZero() {
            // given
            Order order = Order.builder()
                    .roundId(1L)
                    .menuId(10L)
                    .menuName("테스트 메뉴")
                    .menuPrice(BigDecimal.valueOf(15000))
                    .quantity(2)
                    .totalPrice(BigDecimal.valueOf(30000))
                    .build();

            // when
            order.updateQuantity(0);

            // then
            assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
