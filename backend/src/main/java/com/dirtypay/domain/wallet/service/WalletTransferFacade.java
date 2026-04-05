package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import com.dirtypay.global.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 지갑 이체 분산 락 Facade.
 *
 * <p>오너 지갑을 단일 키로 분산 락을 획득하여 동시 정산 송금 시 Lost Update를 방지한다.
 * 여러 페이어가 같은 오너에게 동시 송금할 때 직렬화된다.
 * {@code @DistributedLock}이 붙은 메서드는 {@link com.dirtypay.global.lock.DistributedLockAspect}에 의해
 * Redisson 락 획득 후 {@code REQUIRES_NEW} 트랜잭션으로 실행된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class WalletTransferFacade {

    private final WalletRepository walletRepository;
    private final WalletService walletService;

    /**
     * 오너 지갑에 분산 락을 획득하고 송금을 실행한다.
     *
     * <p>락 키: {@code lock:wallet:{receiverId}}</p>
     *
     * <p>지갑 조회는 락 획득 후 REQUIRES_NEW 트랜잭션 내에서 수행되며,
     * 조회된 지갑 ID를 {@link WalletTransferResult}에 담아 반환한다.
     * 호출자는 이 결과를 사용하여 별도 사전 조회 없이 이체 기록을 구성할 수 있다.</p>
     *
     * @param senderId       송금자 회원 ID
     * @param receiverId     수신자(오너) 회원 ID — 락 키 기준
     * @param amount         송금 금액
     * @param idempotencyKey 중복 방지 키
     * @param referenceType  참조 유형
     * @param referenceId    참조 ID
     * @param description    거래 설명
     * @return 이체에 사용된 송신자·수신자 지갑 ID
     * @throws EntityNotFoundException 지갑이 존재하지 않는 경우
     */
    @DistributedLock(key = "'wallet:' + #receiverId")
    public WalletTransferResult transferWithLock(Long senderId,
                                                 Long receiverId,
                                                 BigDecimal amount,
                                                 String idempotencyKey,
                                                 String referenceType,
                                                 Long referenceId,
                                                 String description) {
        Wallet senderWallet = walletRepository.findByMemberId(senderId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
        Wallet receiverWallet = walletRepository.findByMemberId(receiverId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
        walletService.transferInternal(senderWallet, receiverWallet, amount,
                idempotencyKey, referenceType, referenceId, description);
        return new WalletTransferResult(senderWallet.getId(), receiverWallet.getId());
    }

    /**
     * 오너 지갑에 분산 락을 획득하고 취소 환불 이체를 실행한다.
     *
     * <p>락 키: {@code lock:wallet:{refundSenderMemberId}} (오너 = 환불 방향의 송신자)</p>
     *
     * @param refundSenderMemberId   환불 송신자(오너) 회원 ID — 락 키 기준
     * @param refundReceiverMemberId 환불 수신자(페이어) 회원 ID
     * @param amount                 환불 금액
     * @param idempotencyKey         중복 방지 키
     * @param referenceType          참조 유형
     * @param referenceId            참조 ID
     * @param description            거래 설명
     * @throws EntityNotFoundException 지갑이 존재하지 않는 경우
     */
    @DistributedLock(key = "'wallet:' + #refundSenderMemberId")
    public void cancelTransferWithLock(Long refundSenderMemberId,
                                       Long refundReceiverMemberId,
                                       BigDecimal amount,
                                       String idempotencyKey,
                                       String referenceType,
                                       Long referenceId,
                                       String description) {
        Wallet refundSenderWallet = walletRepository.findByMemberId(refundSenderMemberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
        Wallet refundReceiverWallet = walletRepository.findByMemberId(refundReceiverMemberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
        walletService.transferInternal(refundSenderWallet, refundReceiverWallet, amount,
                idempotencyKey, referenceType, referenceId, description);
    }
}
