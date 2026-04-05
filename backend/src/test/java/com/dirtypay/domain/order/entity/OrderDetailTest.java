package com.dirtypay.domain.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderDetail} 단위 테스트.
 *
 * <p>Builder 생성 및 필드 설정, shareRatio 기본값 처리를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class OrderDetailTest {

    @Test
    @DisplayName("Builder로 생성 시 orderId, orgMemberId, shareRatio가 올바르게 설정된다")
    void createOrderDetail_allFieldsSet() {
        // given & when
        OrderDetail detail = OrderDetail.builder()
                .orderId(1L)
                .orgMemberId(100L)
                .shareRatio(3)
                .build();

        // then
        assertThat(detail.getOrderId()).isEqualTo(1L);
        assertThat(detail.getOrgMemberId()).isEqualTo(100L);
        assertThat(detail.getShareRatio()).isEqualTo(3);
    }

    @Test
    @DisplayName("shareRatio에 null을 전달하면 기본값 1이 설정된다")
    void createOrderDetail_shareRatioDefaultsToOneWhenNull() {
        // given & when
        OrderDetail detail = OrderDetail.builder()
                .orderId(1L)
                .orgMemberId(100L)
                .shareRatio(null)
                .build();

        // then
        assertThat(detail.getShareRatio()).isEqualTo(1);
    }

    @Test
    @DisplayName("shareRatio 1을 명시적으로 전달하면 그대로 1이 설정된다")
    void createOrderDetail_explicitShareRatioOne() {
        // given & when
        OrderDetail detail = OrderDetail.builder()
                .orderId(2L)
                .orgMemberId(200L)
                .shareRatio(1)
                .build();

        // then
        assertThat(detail.getShareRatio()).isEqualTo(1);
    }
}
