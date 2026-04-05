package com.dirtypay.domain.wallet.service;

/**
 * 지갑 이체 실행 결과.
 *
 * <p>{@link WalletTransferFacade#transferWithLock}이 분산 락 + REQUIRES_NEW 트랜잭션 내에서
 * 조회한 송신자·수신자 지갑 ID를 호출자에게 반환한다.
 * 이를 통해 호출자({@link SettlementTransferService})가 별도의 사전 조회 없이
 * {@link com.dirtypay.domain.wallet.entity.SettlementTransfer} 기록을 구성할 수 있다.</p>
 *
 * @param senderWalletId   송신자 지갑 ID
 * @param receiverWalletId 수신자 지갑 ID
 * @author kim-young-woong
 * @since 1.0.0
 */
public record WalletTransferResult(Long senderWalletId, Long receiverWalletId) {
}
