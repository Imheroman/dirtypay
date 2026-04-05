package com.dirtypay.domain.wallet.entity;

/**
 * 지갑 상태 열거형.
 *
 * <p>지갑의 운영 상태를 나타낸다.</p>
 * <ul>
 *   <li>{@link #ACTIVE} — 정상 사용 가능한 지갑. 충전·출금·송금 모두 허용된다.</li>
 *   <li>{@link #FROZEN} — 일시 동결된 지갑. 모든 금전 거래가 제한된다.</li>
 *   <li>{@link #CLOSED} — 폐쇄 처리된 지갑. 폐쇄 후에는 {@link #ACTIVE}로 복구할 수 없다.</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum WalletStatus {

    /**
     * 정상 활성 상태.
     */
    ACTIVE,

    /**
     * 일시 동결 상태.
     */
    FROZEN,

    /**
     * 폐쇄 상태.
     */
    CLOSED
}
