package com.dirtypay.domain.settlement.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementPaymentTest {

    @Test
    @DisplayName("SettlementPayment 생성 시 기본값은 paidAmount=0, isPaid=false이다")
    void createSettlementPayment_defaultValues() {
        // given & when
        SettlementPayment payment = SettlementPayment.builder()
                .sessionId(1L)
                .orgMemberId(10L)
                .build();

        // then
        assertThat(payment.getSessionId()).isEqualTo(1L);
        assertThat(payment.getOrgMemberId()).isEqualTo(10L);
        assertThat(payment.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.isPaid()).isFalse();
    }

    @Nested
    @DisplayName("updatePayment 테스트")
    class UpdatePaymentTest {

        @Test
        @DisplayName("paidAmount < totalAmount이면 isPaid는 false이다")
        void updatePayment_lessThanTotal_notPaid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            // when
            payment.updatePayment(BigDecimal.valueOf(9000), BigDecimal.valueOf(10000));

            // then
            assertThat(payment.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(9000));
            assertThat(payment.isPaid()).isFalse();
        }

        @Test
        @DisplayName("paidAmount == totalAmount이면 isPaid는 true이다")
        void updatePayment_equalToTotal_paid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            // when
            payment.updatePayment(BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));

            // then
            assertThat(payment.getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(payment.isPaid()).isTrue();
        }

        @Test
        @DisplayName("paidAmount > totalAmount이면 isPaid는 true이다")
        void updatePayment_greaterThanTotal_paid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            // when
            payment.updatePayment(BigDecimal.valueOf(15000), BigDecimal.valueOf(10000));

            // then
            assertThat(payment.isPaid()).isTrue();
        }

        @Test
        @DisplayName("0원 납부, 0원 총액이면 isPaid는 true이다 (0 >= 0)")
        void updatePayment_zeroAmounts_paid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            // when
            payment.updatePayment(BigDecimal.ZERO, BigDecimal.ZERO);

            // then
            assertThat(payment.isPaid()).isTrue();
        }

        @Test
        @DisplayName("1원 차이로 미달하면 isPaid는 false이다")
        void updatePayment_oneWonShort_notPaid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            // when
            payment.updatePayment(BigDecimal.valueOf(9999), BigDecimal.valueOf(10000));

            // then
            assertThat(payment.isPaid()).isFalse();
        }

        @Test
        @DisplayName("완납 후 다시 0으로 되돌리면 isPaid는 false이다")
        void updatePayment_revertToZero_notPaid() {
            // given
            SettlementPayment payment = SettlementPayment.builder()
                    .sessionId(1L)
                    .orgMemberId(10L)
                    .build();

            payment.updatePayment(BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));
            assertThat(payment.isPaid()).isTrue();

            // when
            payment.updatePayment(BigDecimal.ZERO, BigDecimal.valueOf(10000));

            // then
            assertThat(payment.isPaid()).isFalse();
        }
    }
}
