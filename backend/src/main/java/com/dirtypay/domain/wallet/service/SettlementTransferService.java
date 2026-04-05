package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.domain.settlement.dto.response.MemberSettlementResponse;
import com.dirtypay.domain.settlement.service.SettlementService;
import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import com.dirtypay.domain.wallet.dto.response.SettlementTransferResponse;
import com.dirtypay.domain.wallet.entity.SettlementTransfer;
import com.dirtypay.domain.wallet.entity.TransferStatus;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.repository.SettlementTransferRepository;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 정산 송금 서비스.
 *
 * <p>세션 내 조직 멤버의 정산 금액을 총무 지갑으로 송금하는 기능을 제공한다.
 * 정산 금액은 {@link SettlementService#calculateMemberSettlement}로 자동 계산되며,
 * 실제 지갑 이체는 {@link WalletTransferFacade}를 통해 Redisson 분산 락 보호 하에 처리된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementTransferService {

    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletTransferFacade walletTransferFacade;
    private final SettlementTransferRepository settlementTransferRepository;
    private final SettlementService settlementService;
    private final OrgMemberRepository orgMemberRepository;
    private final SessionRepository sessionRepository;

    /**
     * 정산 송금을 생성하고 실행한다.
     *
     * <p>다음 순서로 처리한다.</p>
     * <ol>
     *   <li>세션 조회 — 없으면 {@code SESSION_NOT_FOUND}</li>
     *   <li>조직 멤버 조회 — 없으면 {@code MEMBER_NOT_FOUND}</li>
     *   <li>비회원 지갑 없음 검증 — userId가 null이면 {@code MEMBER_NOT_FOUND}</li>
     *   <li>중복 송금 검증 — 이미 처리된 경우 {@code WALLET_DUPLICATE_TRANSACTION}</li>
     *   <li>정산 금액 계산</li>
     *   <li>분산 락(WalletTransferFacade)으로 오너 지갑 직렬화 — 지갑 조회·이체를 단일 트랜잭션 내 수행</li>
     *   <li>{@link SettlementTransfer} 기록 저장(flush) 및 완료 처리 — DB 레벨 UNIQUE 위반 즉시 감지</li>
     *   <li>정산 납부 상태 업데이트</li>
     * </ol>
     *
     * <p>동시 요청 시 check-then-act Race Condition 방어:
     * {@code existsBySessionIdAndOrgMemberId} 검사 이후 동시 스레드가 먼저 삽입할 경우
     * {@link DataIntegrityViolationException}이 발생하며, 이를 {@code WALLET_DUPLICATE_TRANSACTION}
     * 비즈니스 예외로 변환하여 일관된 응답을 반환한다.
     * DB 레벨 부분 UNIQUE 인덱스(active_transfer_key 가상 컬럼)가 최종 방어선으로 작동한다.</p>
     *
     * @param sessionId    세션 ID
     * @param orgMemberId  조직 멤버 ID (송금자)
     * @param strategyType 나머지 분배 전략 유형
     * @return 생성된 정산 송금 응답 DTO
     * @throws EntityNotFoundException       {@code SESSION_NOT_FOUND} — 세션이 존재하지 않는 경우
     * @throws EntityNotFoundException       {@code MEMBER_NOT_FOUND} — 조직 멤버가 존재하지 않거나 비회원인 경우
     * @throws BusinessException             {@code WALLET_DUPLICATE_TRANSACTION} — 이미 처리된 정산 송금이거나 동시 중복 요청인 경우
     */
    @Transactional
    public SettlementTransferResponse createSettlementTransfer(Long sessionId,
                                                                Long orgMemberId,
                                                                RemainderStrategyType strategyType) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));

        OrgMember orgMember = orgMemberRepository.findById(orgMemberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        if (orgMember.getUserId() == null) {
            throw new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (settlementTransferRepository.existsBySessionIdAndOrgMemberId(sessionId, orgMemberId)) {
            throw new BusinessException(ErrorCode.WALLET_DUPLICATE_TRANSACTION);
        }

        MemberSettlementResponse memberSettlement =
                settlementService.calculateMemberSettlement(sessionId, orgMemberId, strategyType);
        BigDecimal amount = memberSettlement.getTotalAmount();

        Long senderId   = orgMember.getUserId();
        Long receiverId = session.getOwnerId();

        // 지갑 조회는 분산 락 + REQUIRES_NEW 트랜잭션 내에서 수행하여 사전 조회 제거
        String idempotencyKey = "settle:" + sessionId + ":" + orgMemberId;
        WalletTransferResult transferResult = walletTransferFacade.transferWithLock(
                senderId, receiverId, amount,
                idempotencyKey, "SETTLEMENT", sessionId, "정산 송금");

        SettlementTransfer transfer = SettlementTransfer.builder()
                .sessionId(sessionId)
                .orgMemberId(orgMemberId)
                .senderWalletId(transferResult.senderWalletId())
                .receiverWalletId(transferResult.receiverWalletId())
                .amount(amount)
                .build();
        try {
            // saveAndFlush로 즉시 flush하여 DB UNIQUE 위반(동시 중복 요청)을 트랜잭션 커밋 전에 감지
            settlementTransferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException e) {
            // DB 레벨 active_transfer_key UNIQUE 인덱스 위반: 동시 중복 송금 Race Condition 방어
            throw new BusinessException(ErrorCode.WALLET_DUPLICATE_TRANSACTION);
        }
        transfer.complete();

        settlementService.updateSettlementPayment(sessionId, orgMemberId, amount, strategyType);

        return SettlementTransferResponse.from(transfer);
    }

    /**
     * 정산 송금을 취소하고 환불 이체를 수행한다.
     *
     * <p>{@link com.dirtypay.domain.wallet.entity.TransferStatus#PENDING} 상태의 송금만 취소할 수 있다.
     * 취소 시 수신자 지갑에서 송신자 지갑으로 역방향 이체가 실행된다.
     * 분산 락(WalletTransferFacade)으로 오너 지갑을 직렬화하여 동시 환불 충돌을 방지한다.
     *
     * <p>락 순서는 {@code createSettlementTransfer}의 {@code complete()} 패턴과 동일하게
     * <em>락 획득 → 비즈니스 로직(cancel) → 락 해제</em> 순서를 유지한다.
     * 이를 통해 {@code cancel()} 호출이 락 내부에서 이뤄져 상태 전이와 환불 이체의 원자성을 보장한다.</p>
     *
     * @param transferId 취소할 정산 송금 ID
     * @param memberId   요청 회원 ID (송금자 본인 검증용)
     * @return 취소된 정산 송금 응답 DTO
     * @throws EntityNotFoundException {@code TRANSFER_NOT_FOUND} — 정산 송금이 존재하지 않는 경우
     * @throws BusinessException       {@code TRANSFER_NOT_CANCELLABLE} — 취소 불가능한 상태인 경우
     * @throws BusinessException       {@code WALLET_NOT_FOUND} — 요청자의 지갑이 존재하지 않는 경우
     * @throws BusinessException       {@code TRANSFER_ACCESS_DENIED} — 송금자 본인이 아닌 경우
     */
    @Transactional
    public SettlementTransferResponse cancelSettlementTransfer(Long transferId, Long memberId) {
        SettlementTransfer transfer = settlementTransferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.TRANSFER_NOT_FOUND));

        Wallet requesterWallet = walletService.getWalletEntity(memberId);
        if (!requesterWallet.getId().equals(transfer.getSenderWalletId())) {
            throw new BusinessException(ErrorCode.TRANSFER_ACCESS_DENIED);
        }

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRANSFER_NOT_CANCELLABLE);
        }

        Long ownerWalletId = transfer.getReceiverWalletId();   // 환불 방향 송신자 = 원래 수신자(오너)
        Long payerWalletId  = transfer.getSenderWalletId();    // 환불 방향 수신자 = 원래 송신자(페이어)

        Long ownerMemberId = walletRepository.findById(ownerWalletId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND))
                .getMemberId();
        Long payerMemberId = walletRepository.findById(payerWalletId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND))
                .getMemberId();

        String refundKey = "refund:settle:" + transfer.getSessionId() + ":" + transfer.getOrgMemberId();
        walletTransferFacade.cancelTransferWithLock(ownerMemberId, payerMemberId, transfer.getAmount(),
                refundKey, "SETTLEMENT_REFUND", transfer.getId(), "정산 송금 취소");

        // 락 획득 후 상태 전이: createSettlementTransfer의 complete() 패턴과 동일한 순서
        transfer.cancel();

        settlementService.updateSettlementPayment(
                transfer.getSessionId(), transfer.getOrgMemberId(),
                BigDecimal.ZERO, RemainderStrategyType.OWNER);

        return SettlementTransferResponse.from(transfer);
    }

    /**
     * 세션의 정산 송금 목록을 조회한다.
     *
     * @param sessionId 조회할 세션 ID
     * @return 해당 세션의 정산 송금 응답 DTO 목록
     */
    public List<SettlementTransferResponse> getSettlementTransfers(Long sessionId) {
        return settlementTransferRepository.findBySessionId(sessionId).stream()
                .map(SettlementTransferResponse::from)
                .toList();
    }
}
