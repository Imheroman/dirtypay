package com.dirtypay.domain.store.entity;

/**
 * 매장 유형 열거형.
 *
 * <p>매장이 POS 시스템과 연동하는 방식을 나타낸다.</p>
 * <ul>
 *   <li>{@link #POS_INTEGRATED} — POS 시스템과 연동된 매장. {@code posIntegrationKey}가 필수다.</li>
 *   <li>{@link #DIRECT} — 직접 운영 매장. POS 연동 없이 수동으로 주문을 관리한다.</li>
 *   <li>{@link #CUSTOM} — 사용자 정의 매장. POS 연동 없이 "나만의 가게" 용도로 사용한다.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum StoreType {

    /**
     * POS 시스템 연동 매장.
     */
    POS_INTEGRATED,

    /**
     * 직접 운영 매장.
     */
    DIRECT,

    /**
     * 사용자 정의 매장.
     */
    CUSTOM
}
