package com.dirtypay.domain.settlement.strategy;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 총무에게 나머지를 할당하는 전략.
 *
 * <p>균등 분배 후 나머지 금액을 총무(세션 소유자)에게 할당한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public class OwnerRemainderStrategy implements RemainderStrategy {

    @Override
    public void apply(Map<Integer, BigDecimal> amounts, BigDecimal remainder, int ownerIndex) {
        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            amounts.merge(ownerIndex, remainder, BigDecimal::add);
        }
    }
}
