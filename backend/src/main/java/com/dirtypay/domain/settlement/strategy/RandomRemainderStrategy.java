package com.dirtypay.domain.settlement.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 랜덤 분배 전략.
 *
 * <p>나머지 금액을 1원씩 랜덤한 참여자에게 분배한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public class RandomRemainderStrategy implements RemainderStrategy {

    @Override
    public void apply(Map<Integer, BigDecimal> amounts, BigDecimal remainder, int ownerIndex) {
        int remainderInt = remainder.intValue();
        if (remainderInt <= 0) {
            return;
        }

        List<Integer> indices = new ArrayList<>(amounts.keySet());
        Collections.shuffle(indices);

        for (int i = 0; i < remainderInt && i < indices.size(); i++) {
            amounts.merge(indices.get(i), BigDecimal.ONE, BigDecimal::add);
        }
    }
}
