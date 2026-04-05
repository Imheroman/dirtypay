package com.dirtypay.domain.wallet.service;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.wallet.dto.request.WalletChargeRequest;
import com.dirtypay.domain.wallet.dto.request.WalletTransferRequest;
import com.dirtypay.domain.wallet.dto.response.WalletResponse;
import com.dirtypay.domain.wallet.dto.response.WalletTransactionResponse;
import com.dirtypay.domain.wallet.entity.TransactionStatus;
import com.dirtypay.domain.wallet.entity.TransactionType;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.entity.WalletTransaction;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.domain.wallet.repository.WalletTransactionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 지갑 서비스.
 *
 * <p>지갑 생성, 조회, 충전 및 거래 이력 조회 기능을 제공한다.
 * 일일 충전 한도({@code DAILY_CHARGE_LIMIT})를 초과하는 충전은 허용하지 않는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private static final BigDecimal DAILY_CHARGE_LIMIT = new BigDecimal("3000000");

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;

    /**
     * 회원 지갑을 생성한다.
     *
     * <p>이미 지갑이 존재하는 회원에게는 지갑을 중복 생성할 수 없다.</p>
     *
     * @param memberId 지갑을 생성할 회원 ID
     * @return 생성된 지갑 정보 응답 DTO
     * @throws BusinessException {@code WALLET_ALREADY_EXISTS} — 이미 지갑이 존재하는 경우
     */
    @Transactional
    public WalletResponse createWallet(Long memberId) {
        if (walletRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.WALLET_ALREADY_EXISTS);
        }
        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .build();
        walletRepository.save(wallet);
        return WalletResponse.from(wallet);
    }

    /**
     * 회원 지갑 정보를 조회한다.
     *
     * @param memberId 조회할 회원 ID
     * @return 지갑 정보 응답 DTO
     * @throws EntityNotFoundException {@code WALLET_NOT_FOUND} — 지갑이 존재하지 않는 경우
     */
    public WalletResponse getWallet(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
        return WalletResponse.from(wallet);
    }

    /**
     * 회원 지갑 엔티티를 조회한다. (내부 서비스 간 공유용)
     *
     * @param memberId 조회할 회원 ID
     * @return 지갑 엔티티
     * @throws EntityNotFoundException {@code WALLET_NOT_FOUND} — 지갑이 존재하지 않는 경우
     */
    public Wallet getWalletEntity(Long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.WALLET_NOT_FOUND));
    }

    /**
     * 지갑에 금액을 충전한다.
     *
     * <p>충전 전 일일 한도를 확인하고, 초과하는 경우 예외를 던진다.
     * 충전 성공 시 거래 이력이 {@link TransactionType#CHARGE} 타입으로 기록된다.</p>
     *
     * @param memberId 충전할 회원 ID
     * @param request  충전 요청 DTO
     * @return 충전 후 지갑 정보 응답 DTO
     * @throws EntityNotFoundException {@code WALLET_NOT_FOUND} — 지갑이 존재하지 않는 경우
     * @throws BusinessException {@code WALLET_DAILY_LIMIT_EXCEEDED} — 일일 충전 한도를 초과한 경우
     */
    @Transactional
    public WalletResponse charge(Long memberId, WalletChargeRequest request) {
        Wallet wallet = getWalletEntity(memberId);
        wallet.resetDailyLimitIfNeeded();

        BigDecimal amount = request.getAmount();
        if (wallet.getDailyChargedAmount().add(amount).compareTo(DAILY_CHARGE_LIMIT) > 0) {
            throw new BusinessException(ErrorCode.WALLET_DAILY_LIMIT_EXCEEDED);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.charge(amount);
        BigDecimal balanceAfter = wallet.getBalance();

        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .type(TransactionType.CHARGE)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .idempotencyKey("charge:" + UUID.randomUUID())
                .status(TransactionStatus.COMPLETED)
                .build();
        walletTransactionRepository.save(transaction);

        return WalletResponse.from(wallet);
    }

    /**
     * 지갑 거래 이력을 페이징 조회한다.
     *
     * @param walletId 조회할 지갑 ID
     * @param pageable 페이징 정보
     * @return 거래 이력 Page
     */
    public Page<WalletTransactionResponse> getTransactions(Long walletId, Pageable pageable) {
        return walletTransactionRepository
                .findByWalletIdOrderByCreatedDateDesc(walletId, pageable)
                .map(WalletTransactionResponse::from);
    }

    /**
     * 송신자 지갑에서 수신자 지갑으로 금액을 송금한다.
     *
     * <p>멱등성 키가 null이면 {@code "transfer:" + UUID}로 자동 생성된다.
     * 이미 처리된 멱등성 키가 제공되면 기존 {@link TransactionType#TRANSFER_OUT} 거래를 반환한다.
     * 같은 지갑으로의 송금은 허용하지 않는다.</p>
     *
     * <p>1차 구현으로 낙관적/비관적 Lock 없이 단순 구현한다.</p>
     *
     * @param senderMemberId 송신자 회원 ID
     * @param request        송금 요청 DTO
     * @return TRANSFER_OUT 거래의 응답 DTO
     * @throws EntityNotFoundException {@code MEMBER_NOT_FOUND} — 수신자 이메일에 해당하는 회원이 없는 경우
     * @throws EntityNotFoundException {@code WALLET_NOT_FOUND} — 송신자 또는 수신자 지갑이 없는 경우
     * @throws BusinessException       {@code WALLET_TRANSFER_SAME_WALLET} — 같은 지갑으로 송금하는 경우
     * @throws BusinessException       {@code WALLET_INSUFFICIENT_BALANCE} — 송신자 잔액이 부족한 경우
     */
    @Transactional
    public WalletTransactionResponse transfer(Long senderMemberId, WalletTransferRequest request) {
        Wallet sender = getWalletEntity(senderMemberId);

        Member receiverMember = memberRepository.findByEmail(request.getReceiverEmail())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        Wallet receiver = getWalletEntity(receiverMember.getId());

        if (sender.getId().equals(receiver.getId())) {
            throw new BusinessException(ErrorCode.WALLET_TRANSFER_SAME_WALLET);
        }

        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : "transfer:" + UUID.randomUUID();

        if (request.getIdempotencyKey() != null) {
            return walletTransactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(WalletTransactionResponse::from)
                    .orElseGet(() -> executeTransfer(sender, receiver, request.getAmount(), idempotencyKey, request.getDescription()));
        }

        return executeTransfer(sender, receiver, request.getAmount(), idempotencyKey, request.getDescription());
    }

    /**
     * 실제 송금 거래를 실행하고 TRANSFER_OUT 거래 응답 DTO를 반환한다.
     *
     * @param sender         송신자 지갑
     * @param receiver       수신자 지갑
     * @param amount         송금 금액
     * @param idempotencyKey 멱등성 키
     * @param description    거래 설명 (nullable)
     * @return TRANSFER_OUT 거래의 응답 DTO
     */
    private WalletTransactionResponse executeTransfer(
            Wallet sender, Wallet receiver, BigDecimal amount, String idempotencyKey, String description) {
        WalletTransaction outTx = transferInternal(sender, receiver, amount, idempotencyKey, null, null, description);
        return WalletTransactionResponse.from(outTx);
    }

    /**
     * 두 지갑 간 내부 송금 처리를 수행하고 TRANSFER_OUT 거래를 반환한다.
     *
     * <p>정산 송금({@code SettlementTransferService}) 등 다른 서비스에서 재사용할 수 있도록
     * package-private으로 공개한다. 호출 측에서 트랜잭션 컨텍스트를 보장해야 한다.</p>
     *
     * @param sender         송신자 지갑
     * @param receiver       수신자 지갑
     * @param amount         송금 금액
     * @param idempotencyKey 멱등성 키 (TRANSFER_OUT에 사용, TRANSFER_IN은 ":in" suffix 추가)
     * @param referenceType  연관 엔티티 타입 (nullable, 예: "SETTLEMENT")
     * @param referenceId    연관 엔티티 ID (nullable)
     * @param description    거래 설명 (nullable)
     * @return 저장된 TRANSFER_OUT {@link WalletTransaction}
     */
    WalletTransaction transferInternal(
            Wallet sender,
            Wallet receiver,
            BigDecimal amount,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String description) {

        BigDecimal senderBalanceBefore = sender.getBalance();
        sender.withdraw(amount);
        BigDecimal senderBalanceAfter = sender.getBalance();

        BigDecimal receiverBalanceBefore = receiver.getBalance();
        receiver.deposit(amount);
        BigDecimal receiverBalanceAfter = receiver.getBalance();

        WalletTransaction outTx = WalletTransaction.builder()
                .walletId(sender.getId())
                .type(TransactionType.TRANSFER_OUT)
                .amount(amount)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceAfter)
                .counterpartyWalletId(receiver.getId())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .build();

        WalletTransaction inTx = WalletTransaction.builder()
                .walletId(receiver.getId())
                .type(TransactionType.TRANSFER_IN)
                .amount(amount)
                .balanceBefore(receiverBalanceBefore)
                .balanceAfter(receiverBalanceAfter)
                .counterpartyWalletId(sender.getId())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey + ":in")
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .build();

        walletTransactionRepository.save(outTx);
        walletTransactionRepository.save(inTx);

        return outTx;
    }
}
