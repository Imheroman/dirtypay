package com.dirtypay.domain.store.entity;

/**
 * 매장 주문 상태 열거형.
 *
 * <p>매장 주문의 처리 단계를 나타낸다. 상태 전이는 아래와 같다:</p>
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *         ↓
 *      CANCELLED
 * </pre>
 * <ul>
 *   <li>{@link #PENDING} — 주문 접수 대기 중. 초기 상태.</li>
 *   <li>{@link #CONFIRMED} — 매장에서 주문을 확인하고 처리 중.</li>
 *   <li>{@link #COMPLETED} — 주문 처리 완료.</li>
 *   <li>{@link #CANCELLED} — 주문 취소. PENDING 또는 CONFIRMED 상태에서만 취소 가능.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum StoreOrderStatus {

    /**
     * 주문 접수 대기.
     */
    PENDING,

    /**
     * 주문 확인 및 처리 중.
     */
    CONFIRMED,

    /**
     * 주문 처리 완료.
     */
    COMPLETED,

    /**
     * 주문 취소.
     */
    CANCELLED
}
