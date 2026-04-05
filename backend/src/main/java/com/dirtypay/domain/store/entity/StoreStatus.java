package com.dirtypay.domain.store.entity;

/**
 * 매장 상태 열거형.
 *
 * <p>매장의 운영 상태를 나타낸다.</p>
 * <ul>
 *   <li>{@link #ACTIVE} — 정상 운영 중인 매장.</li>
 *   <li>{@link #INACTIVE} — 일시 휴업 또는 비활성화된 매장.</li>
 *   <li>{@link #CLOSED} — 폐업 처리된 매장. 폐업 후에는 {@link #ACTIVE}로 복구할 수 없다.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum StoreStatus {

    /**
     * 정상 운영 중.
     */
    ACTIVE,

    /**
     * 일시 비활성화.
     */
    INACTIVE,

    /**
     * 폐업.
     */
    CLOSED
}
