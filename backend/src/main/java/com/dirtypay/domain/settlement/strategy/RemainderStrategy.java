package com.dirtypay.domain.settlement.strategy;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 나머지 분배 전략 인터페이스.
 *
 * <p>균등 분배 후 발생하는 나머지 금액을 처리하는 전략을 정의한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RemainderStrategy {

    /**
     * 나머지 금액을 분배한다.
     *
     * @param amounts    인덱스별 분배 금액 (수정 가능)
     * @param remainder  나머지 금액
     * @param ownerIndex 총무(소유자) 인덱스
     */
    void apply(Map<Integer, BigDecimal> amounts, BigDecimal remainder, int ownerIndex);
}
