package com.dirtypay.domain.wallet.service;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.wallet.entity.TransactionType;
import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.entity.WalletTransaction;
import com.dirtypay.domain.wallet.helper.WalletTestFixture;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.domain.wallet.repository.WalletTransactionRepository;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link WalletTransferFacade} 통합 테스트.
 *
 * <p>실제 Redis(Testcontainers) + MariaDB(Testcontainers) 환경에서
 * 분산 락이 적용된 이체·취소 로직이 올바르게 동작하는지 검증한다.
 * {@code @DistributedLock}이 AOP를 통해 Redisson 락 획득 후 REQUIRES_NEW 트랜잭션으로
 * 실행되므로, balance 검증 전 {@code entityManager.clear()}를 통해 1차 캐시를 초기화한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, WalletTestFixture.class})
@Testcontainers
@DisplayName("WalletTransferFacadeIntegrationTest 통합 테스트")
class WalletTransferFacadeIntegrationTest {

    @Autowired
    private WalletTransferFacade walletTransferFacade;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletTestFixture walletTestFixture;

    @PersistenceContext
    private EntityManager entityManager;

    // TC-1, TC-3, TC-4 공용 기본 테스트 데이터
    private Member sender;
    private Member receiver;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = walletTestFixture.createMember("sender@test.com", "Sender");
        receiver = walletTestFixture.createMember("receiver@test.com", "Receiver");
        senderWallet = walletTestFixture.createWallet(sender.getId(), new BigDecimal("100000"));
        receiverWallet = walletTestFixture.createWallet(receiver.getId(), BigDecimal.ZERO);
    }

    @AfterEach
    void tearDown() {
        walletTestFixture.cleanupAll();
    }

    // =========================================================
    // TC-1
    // =========================================================

    @Test
    @DisplayName("transferWithLock 호출 시 sender 잔액 감소, receiver 잔액 증가가 DB에 반영된다")
    void transferWithLock_balanceChangedCorrectly() {
        // given
        // sender balance=100,000, receiver balance=0 (setUp에서 초기화)

        // when
        walletTransferFacade.transferWithLock(
                sender.getId(), receiver.getId(),
                new BigDecimal("30000"),
                "idem:tc1", "TEST", 1L, "TC1 이체");

        // then
        entityManager.clear();
        assertThat(walletRepository.findByMemberId(sender.getId()).get().getBalance())
                .isEqualByComparingTo("70000");
        assertThat(walletRepository.findByMemberId(receiver.getId()).get().getBalance())
                .isEqualByComparingTo("30000");
    }

    // =========================================================
    // TC-2
    // =========================================================

    @Test
    @DisplayName("cancelTransferWithLock 호출 시 환불 방향으로 잔액이 정확히 이동한다")
    void cancelTransferWithLock_balanceMovedInRefundDirection() {
        // given
        Member owner = walletTestFixture.createMember("owner@test.com", "Owner");
        Member payer = walletTestFixture.createMember("payer@test.com", "Payer");
        walletTestFixture.createWallet(owner.getId(), new BigDecimal("50000"));
        walletTestFixture.createWallet(payer.getId(), new BigDecimal("50000"));

        // when
        walletTransferFacade.cancelTransferWithLock(
                owner.getId(), payer.getId(),
                new BigDecimal("30000"),
                "idem:tc2", "TEST", 2L, "TC2 환불");

        // then
        entityManager.clear();
        assertThat(walletRepository.findByMemberId(owner.getId()).get().getBalance())
                .isEqualByComparingTo("20000");
        assertThat(walletRepository.findByMemberId(payer.getId()).get().getBalance())
                .isEqualByComparingTo("80000");
    }

    // =========================================================
    // TC-3
    // =========================================================

    @Test
    @DisplayName("이체 성공 시 TRANSFER_OUT과 TRANSFER_IN 거래 기록이 각각 생성된다")
    void transferWithLock_createsTransferOutAndTransferInRecords() {
        // when
        walletTransferFacade.transferWithLock(
                sender.getId(), receiver.getId(),
                new BigDecimal("10000"),
                "idem:tc3", "TEST", 3L, "TC3 이체");

        // then
        List<WalletTransaction> transactions = walletTransactionRepository.findAll();

        WalletTransaction outTx = transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("TRANSFER_OUT 거래 기록이 없습니다"));

        WalletTransaction inTx = transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow(() -> new AssertionError("TRANSFER_IN 거래 기록이 없습니다"));

        assertThat(outTx.getIdempotencyKey()).isEqualTo("idem:tc3");
        assertThat(inTx.getIdempotencyKey()).isEqualTo("idem:tc3:in");
        assertThat(outTx.getWalletId()).isEqualTo(senderWallet.getId());
        assertThat(inTx.getWalletId()).isEqualTo(receiverWallet.getId());
    }

    // =========================================================
    // TC-4
    // =========================================================

    @Test
    @DisplayName("존재하지 않는 memberId로 이체 시 EntityNotFoundException이 발생한다")
    void transferWithLock_nonExistentSender_throwsEntityNotFoundException() {
        // given
        // senderId = 99999L (존재하지 않음)

        // when & then
        assertThatThrownBy(() ->
                walletTransferFacade.transferWithLock(
                        99999L, receiver.getId(),
                        BigDecimal.TEN,
                        "idem:tc4", "TEST", 4L, "TC4"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // =========================================================
    // TC-5
    // =========================================================

    @Test
    @DisplayName("잔액 부족 시 WALLET_INSUFFICIENT_BALANCE 예외가 전파되고 잔액은 변경되지 않는다")
    void transferWithLock_insufficientBalance_throwsBusinessExceptionAndBalanceUnchanged() {
        // given
        Member poorSender = walletTestFixture.createMember("poor@test.com", "PoorSender");
        walletTestFixture.createWallet(poorSender.getId(), new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() ->
                walletTransferFacade.transferWithLock(
                        poorSender.getId(), receiver.getId(),
                        new BigDecimal("50000"),
                        "idem:tc5", "TEST", 5L, "TC5"))
                .isInstanceOf(BusinessException.class);

        entityManager.clear();
        assertThat(walletRepository.findByMemberId(poorSender.getId()).get().getBalance())
                .isEqualByComparingTo("10000");
    }
}
