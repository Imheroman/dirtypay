package com.dirtypay.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 기반 금액 분할 계산 유틸리티.
 *
 * <p>더치페이 서비스의 핵심인 금액 분할 계산에서
 * 정확한 나눗셈과 나머지 처리를 보장한다.</p>
 *
 * <p>기본 반올림 정책은 {@link RoundingMode#DOWN}이며,
 * 나머지 금액은 별도 필드로 반환하여 호출자가 처리 방식을 결정한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public final class MoneyCalculator {

    private MoneyCalculator() {
        throw new AssertionError("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    /**
     * 금액 분할 결과를 담는 불변 레코드.
     *
     * @param quotient  1인당 몫 (RoundingMode.DOWN 적용)
     * @param remainder 나머지 금액 (totalAmount - quotient * count)
     */
    public record DivisionResult(BigDecimal quotient, BigDecimal remainder) {
    }

    /**
     * 총 금액을 인원 수로 나누어 몫과 나머지를 반환한다.
     *
     * <p>{@link RoundingMode#DOWN}으로 나눈 몫과 나머지를 {@link DivisionResult}로 반환한다.
     * 항상 {@code quotient * count + remainder == totalAmount}가 보장된다.</p>
     *
     * @param totalAmount 분할할 총 금액 (양수여야 한다)
     * @param count       분할 인원 수 (1 이상이어야 한다)
     * @return 몫과 나머지를 담은 {@link DivisionResult}
     * @throws IllegalArgumentException totalAmount가 null, 0 이하이거나 count가 0 이하인 경우
     */
    public static DivisionResult divide(BigDecimal totalAmount, int count) {
        validateAmount(totalAmount);
        validateCount(count);

        BigDecimal divisor = BigDecimal.valueOf(count);
        BigDecimal quotient = totalAmount.divide(divisor, 0, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(quotient.multiply(divisor));

        return new DivisionResult(quotient, remainder);
    }

    private static void validateAmount(BigDecimal totalAmount) {
        if (totalAmount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다.");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다. 입력값: " + totalAmount);
        }
    }

    private static void validateCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("인원 수는 0보다 커야 합니다. 입력값: " + count);
        }
    }
}
