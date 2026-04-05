package com.dirtypay.domain.wallet.entity;

/**
 * 지갑 거래 유형 열거형.
 *
 * <p>지갑에서 발생하는 모든 거래의 유형을 나타낸다.</p>
 * <ul>
 *   <li>{@link #CHARGE} — 외부 수단(카드, 계좌 등)으로 지갑에 금액을 충전한다.</li>
 *   <li>{@link #TRANSFER_OUT} — 다른 회원의 지갑으로 금액을 송금한다(출금 측).</li>
 *   <li>{@link #TRANSFER_IN} — 다른 회원의 지갑으로부터 금액을 수신한다(입금 측).</li>
 *   <li>{@link #REFUND} — 정산 취소 등 사유로 지갑에 금액이 환불된다.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum TransactionType {

    /**
     * 충전 — 외부 수단으로 지갑 잔액을 증가시킨다.
     */
    CHARGE,

    /**
     * 송금 출금 — 상대방 지갑으로 금액을 이체하여 잔액이 감소한다.
     */
    TRANSFER_OUT,

    /**
     * 송금 입금 — 상대방 지갑으로부터 금액을 수신하여 잔액이 증가한다.
     */
    TRANSFER_IN,

    /**
     * 환불 — 정산 취소 등의 사유로 지갑 잔액이 복원된다.
     */
    REFUND
}
