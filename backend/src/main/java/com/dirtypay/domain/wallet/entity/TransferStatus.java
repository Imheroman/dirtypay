package com.dirtypay.domain.wallet.entity;

/**
 * 정산 송금 상태 열거형.
 *
 * <p>정산 송금 트랜잭션의 처리 상태를 나타낸다.</p>
 * <ul>
 *   <li>{@link #PENDING} — 송금이 요청되었으나 아직 처리되지 않은 상태.</li>
 *   <li>{@link #COMPLETED} — 송금이 정상적으로 완료된 상태.</li>
 *   <li>{@link #FAILED} — 송금 처리 중 오류가 발생하여 실패한 상태.</li>
 *   <li>{@link #CANCELLED} — 송금이 취소된 상태. {@link #PENDING} 상태에서만 취소할 수 있다.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum TransferStatus {

    /**
     * 송금 대기 중.
     */
    PENDING,

    /**
     * 송금 완료.
     */
    COMPLETED,

    /**
     * 송금 실패.
     */
    FAILED,

    /**
     * 송금 취소.
     */
    CANCELLED
}
