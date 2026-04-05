package com.dirtypay.domain.wallet.service;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.wallet.helper.WalletTestFixture;
import com.dirtypay.domain.wallet.repository.WalletRepository;
import com.dirtypay.domain.wallet.repository.WalletTransactionRepository;
import com.dirtypay.global.lock.LockAcquisitionFailedException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 멀티스레드 환경에서 분산 락이 Lost Update를 방지하고 잔액 정합성을 보장하는지 검증하는 동시성 테스트.
 *
 * <p>분산 락 도입의 핵심 목적을 직접 증명한다.
 * {@code @Transactional}을 테스트 메서드에 사용하지 않으며, 각 테스트 케이스 내부에서
 * 직접 데이터를 생성하여 멀티스레드 간 데이터 충돌을 방지한다.
 * {@code @DistributedLock}이 REQUIRES_NEW 트랜잭션으로 커밋되므로
 * 잔액 검증 전 {@code entityManager.clear()}로 1차 캐시를 초기화한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, WalletTestFixture.class})
@Testcontainers
@DisplayName("WalletConcurrencyTest 동시성 테스트")
class WalletConcurrencyTest {

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

    @AfterEach
    void tearDown() {
        walletTestFixture.cleanupAll();
    }

    // =========================================================
    // TC-1
    // =========================================================

    @Test
    @DisplayName("10개 스레드가 동시에 같은 receiver에게 1,000원씩 이체하면 최종 잔액이 정확히 10,000원이다")
    void concurrentTransfer_tenSendersToOneReceiver_balanceExactlyTenThousand() throws InterruptedException {
        // given
        Member receiver = walletTestFixture.createMember("receiver-tc1@test.com", "Receiver");
        walletTestFixture.createWallet(receiver.getId(), BigDecimal.ZERO);

        int threadCount = 10;
        Long[] senderIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Member sender = walletTestFixture.createMember("sender-tc1-" + i + "@test.com", "Sender" + i);
            walletTestFixture.createWallet(sender.getId(), new BigDecimal("100000"));
            senderIds[i] = sender.getId();
        }

        Long receiverId = receiver.getId();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    walletTransferFacade.transferWithLock(
                            senderIds[idx], receiverId,
                            new BigDecimal("1000"),
                            "idem:tc1:" + idx,
                            "TEST", 1L, "동시 이체 TC1");
                    successCount.incrementAndGet();
                } catch (LockAcquisitionFailedException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // then
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(10);

        entityManager.clear();
        assertThat(walletRepository.findByMemberId(receiverId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000");
        for (int i = 0; i < threadCount; i++) {
            assertThat(walletRepository.findByMemberId(senderIds[i]).orElseThrow().getBalance())
                    .isEqualByComparingTo("99000");
        }
    }

    // =========================================================
    // TC-2
    // =========================================================

    @Test
    @DisplayName("5개 스레드가 동시에 같은 owner로부터 환불 시 잔액이 정확히 차감된다")
    void concurrentCancelTransfer_fivePayersFromOneOwner_balanceExactlyFiftyThousand() throws InterruptedException {
        // given
        Member owner = walletTestFixture.createMember("owner-tc2@test.com", "Owner");
        walletTestFixture.createWallet(owner.getId(), new BigDecimal("100000"));

        int threadCount = 5;
        Long[] payerIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Member payer = walletTestFixture.createMember("payer-tc2-" + i + "@test.com", "Payer" + i);
            walletTestFixture.createWallet(payer.getId(), BigDecimal.ZERO);
            payerIds[i] = payer.getId();
        }

        Long ownerId = owner.getId();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    walletTransferFacade.cancelTransferWithLock(
                            ownerId, payerIds[idx],
                            new BigDecimal("10000"),
                            "idem:tc2:" + idx,
                            "TEST", 2L, "동시 환불 TC2");
                    successCount.incrementAndGet();
                } catch (LockAcquisitionFailedException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(5);

        entityManager.clear();
        assertThat(walletRepository.findByMemberId(ownerId).orElseThrow().getBalance())
                .isEqualByComparingTo("50000");
        for (int i = 0; i < threadCount; i++) {
            assertThat(walletRepository.findByMemberId(payerIds[i]).orElseThrow().getBalance())
                    .isEqualByComparingTo("10000");
        }
    }

    // =========================================================
    // TC-3
    // =========================================================

    @Test
    @DisplayName("서로 다른 receiverId에 대한 이체는 락 경합 없이 병렬 처리된다")
    void concurrentTransfer_differentReceivers_processedInParallelWithoutLockContention() throws InterruptedException {
        // given
        Member senderA = walletTestFixture.createMember("sender-a-tc3@test.com", "SenderA");
        Member senderB = walletTestFixture.createMember("sender-b-tc3@test.com", "SenderB");
        Member receiverA = walletTestFixture.createMember("receiver-a-tc3@test.com", "ReceiverA");
        Member receiverB = walletTestFixture.createMember("receiver-b-tc3@test.com", "ReceiverB");

        walletTestFixture.createWallet(senderA.getId(), new BigDecimal("100000"));
        walletTestFixture.createWallet(senderB.getId(), new BigDecimal("100000"));
        walletTestFixture.createWallet(receiverA.getId(), BigDecimal.ZERO);
        walletTestFixture.createWallet(receiverB.getId(), BigDecimal.ZERO);

        Long senderAId = senderA.getId();
        Long senderBId = senderB.getId();
        Long receiverAId = receiverA.getId();
        Long receiverBId = receiverB.getId();

        // when
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                walletTransferFacade.transferWithLock(
                        senderAId, receiverAId,
                        new BigDecimal("5000"),
                        "idem:tc3:a",
                        "TEST", 3L, "병렬 이체 TC3-A");
                successCount.incrementAndGet();
            } catch (LockAcquisitionFailedException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                walletTransferFacade.transferWithLock(
                        senderBId, receiverBId,
                        new BigDecimal("7000"),
                        "idem:tc3:b",
                        "TEST", 3L, "병렬 이체 TC3-B");
                successCount.incrementAndGet();
            } catch (LockAcquisitionFailedException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        readyLatch.await();
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(errors).isEmpty();

        entityManager.clear();
        assertThat(walletRepository.findByMemberId(receiverAId).orElseThrow().getBalance())
                .isEqualByComparingTo("5000");
        assertThat(walletRepository.findByMemberId(receiverBId).orElseThrow().getBalance())
                .isEqualByComparingTo("7000");
    }

    // =========================================================
    // TC-5 (대량)
    // =========================================================

    @Test
    @DisplayName("50개 스레드가 동시에 같은 receiver에게 이체하면 성공한 건수만큼 잔액이 정확히 증가한다")
    void concurrentTransfer_fiftyThreadsToOneReceiver_balanceMatchesSuccessCount() throws InterruptedException {
        // given
        Member receiver = walletTestFixture.createMember("receiver-tc5@test.com", "Receiver");
        walletTestFixture.createWallet(receiver.getId(), BigDecimal.ZERO);

        int threadCount = 50;
        Long[] senderIds = new Long[threadCount];
        for (int i = 0; i < threadCount; i++) {
            Member sender = walletTestFixture.createMember("sender-tc5-" + i + "@test.com", "Sender" + i);
            walletTestFixture.createWallet(sender.getId(), new BigDecimal("100000"));
            senderIds[i] = sender.getId();
        }

        Long receiverId = receiver.getId();

        // when
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    walletTransferFacade.transferWithLock(
                            senderIds[idx], receiverId,
                            new BigDecimal("1000"),
                            "idem:tc5:" + idx,
                            "TEST", 5L, "대량 동시 이체 TC5");
                    successCount.incrementAndGet();
                } catch (LockAcquisitionFailedException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS));
        executor.shutdown();

        // then
        assertThat(successCount.get() + failCount.get()).isEqualTo(50);

        entityManager.clear();
        BigDecimal expectedBalance = new BigDecimal(successCount.get()).multiply(new BigDecimal("1000"));
        assertThat(walletRepository.findByMemberId(receiverId).orElseThrow().getBalance())
                .isEqualByComparingTo(expectedBalance);
    }
}
