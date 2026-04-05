package com.dirtypay.domain.settlement.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 10원 단위 올림 전략.
 *
 * <p>각 참여자의 금액을 10원 단위로 올림하고,
 * 총무가 차액을 부담한다 (마이너스가 될 수 있음).</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public class RoundUpRemainderStrategy implements RemainderStrategy {

    private static final BigDecimal TEN = BigDecimal.TEN;

    @Override
    public void apply(Map<Integer, BigDecimal> amounts, BigDecimal remainder, int ownerIndex) {
        BigDecimal totalBeforeRound = amounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(remainder);

        BigDecimal sumAfterRound = BigDecimal.ZERO;

        for (Map.Entry<Integer, BigDecimal> entry : amounts.entrySet()) {
            if (entry.getKey() == ownerIndex) {
                continue;
            }
            BigDecimal rounded = roundUpToTen(entry.getValue());
            entry.setValue(rounded);
            sumAfterRound = sumAfterRound.add(rounded);
        }

        BigDecimal ownerAmount = totalBeforeRound.subtract(sumAfterRound);
        amounts.put(ownerIndex, ownerAmount);
    }

    private BigDecimal roundUpToTen(BigDecimal value) {
        return value.divide(TEN, 0, RoundingMode.UP).multiply(TEN);
    }
}
