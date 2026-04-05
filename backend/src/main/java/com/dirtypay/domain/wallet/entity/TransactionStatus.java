package com.dirtypay.domain.wallet.entity;

/**
 * 지갑 거래 상태 열거형.
 *
 * <p>지갑 거래 이력의 처리 결과 상태를 나타낸다.</p>
 * <ul>
 *   <li>{@link #COMPLETED} — 거래가 정상적으로 완료된 상태.</li>
 *   <li>{@link #FAILED} — 잔액 부족, 한도 초과 등의 사유로 거래가 실패한 상태.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum TransactionStatus {

    /**
     * 거래 완료.
     */
    COMPLETED,

    /**
     * 거래 실패.
     */
    FAILED
}
