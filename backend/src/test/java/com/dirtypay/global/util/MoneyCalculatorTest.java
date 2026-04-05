package com.dirtypay.global.util;

import com.dirtypay.global.util.MoneyCalculator.DivisionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyCalculatorTest {

    @Test
    @DisplayName("균등 분할: 10,000원 / 5명 = 2,000원, 나머지 0원")
    void divide_evenSplit() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = 5;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        assertThat(result.quotient()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.remainder()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("나머지 발생: 10,000원 / 3명 = 3,333원, 나머지 1원")
    void divide_withRemainder() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = 3;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        assertThat(result.quotient()).isEqualByComparingTo(new BigDecimal("3333"));
        assertThat(result.remainder()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("총합 정합성: 몫 x 인원 + 나머지 = 원금")
    void divide_totalIntegrity() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = 3;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        BigDecimal reconstructed = result.quotient()
                .multiply(BigDecimal.valueOf(count))
                .add(result.remainder());
        assertThat(reconstructed).isEqualByComparingTo(totalAmount);
    }

    @Test
    @DisplayName("1원 분할: 1원 / 3명 = 0원, 나머지 1원")
    void divide_oneWon() {
        // given
        BigDecimal totalAmount = BigDecimal.ONE;
        int count = 3;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        assertThat(result.quotient()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.remainder()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("0원 입력 시 IllegalArgumentException 발생")
    void divide_zeroAmount_throwsException() {
        // given
        BigDecimal totalAmount = BigDecimal.ZERO;
        int count = 3;

        // when & then
        assertThatThrownBy(() -> MoneyCalculator.divide(totalAmount, count))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("음수 금액 입력 시 IllegalArgumentException 발생")
    void divide_negativeAmount_throwsException() {
        // given
        BigDecimal totalAmount = new BigDecimal("-1000");
        int count = 3;

        // when & then
        assertThatThrownBy(() -> MoneyCalculator.divide(totalAmount, count))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("0명 분할 시 IllegalArgumentException 발생")
    void divide_zeroCount_throwsException() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = 0;

        // when & then
        assertThatThrownBy(() -> MoneyCalculator.divide(totalAmount, count))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("인원 수는 0보다 커야 합니다");
    }

    @Test
    @DisplayName("null 금액 입력 시 IllegalArgumentException 발생")
    void divide_nullAmount_throwsException() {
        // when & then
        assertThatThrownBy(() -> MoneyCalculator.divide(null, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 null일 수 없습니다");
    }

    @Test
    @DisplayName("음수 인원 입력 시 IllegalArgumentException 발생")
    void divide_negativeCount_throwsException() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = -1;

        // when & then
        assertThatThrownBy(() -> MoneyCalculator.divide(totalAmount, count))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("인원 수는 0보다 커야 합니다");
    }

    @Test
    @DisplayName("1명 분할: 몫 = 원금, 나머지 = 0")
    void divide_singlePerson() {
        // given
        BigDecimal totalAmount = new BigDecimal("10000");
        int count = 1;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        assertThat(result.quotient()).isEqualByComparingTo(totalAmount);
        assertThat(result.remainder()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("큰 금액 분할: 100,000,000원 / 7명 정합성 검증")
    void divide_largeAmount() {
        // given
        BigDecimal totalAmount = new BigDecimal("100000000");
        int count = 7;

        // when
        DivisionResult result = MoneyCalculator.divide(totalAmount, count);

        // then
        BigDecimal reconstructed = result.quotient()
                .multiply(BigDecimal.valueOf(count))
                .add(result.remainder());
        assertThat(reconstructed).isEqualByComparingTo(totalAmount);
        assertThat(result.remainder().compareTo(BigDecimal.valueOf(count))).isLessThan(0);
    }
}
