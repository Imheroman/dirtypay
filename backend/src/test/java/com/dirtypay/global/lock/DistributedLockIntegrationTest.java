package com.dirtypay.global.lock;

import com.dirtypay.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DistributedLock} 어노테이션 통합 테스트.
 *
 * <p>실제 Redis(Testcontainers) + Spring AOP 환경에서 @DistributedLock의
 * 전체 파이프라인을 검증한다. 락 획득, 대기 타임아웃, 독립 키 동시 처리,
 * leaseTime 자동 해제, 완료 후 키 정리, 경합 실패를 포괄한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, DistributedLockIntegrationTest.LockTestConfig.class})
@Testcontainers
@DisplayName("DistributedLockIntegrationTest 통합 테스트")
class DistributedLockIntegrationTest {

    @Autowired
    private LockTestService lockTestService;

    @Autowired
    private RedissonClient redissonClient;

    // -------------------------------------------------------------------------
    // Inner TestConfiguration
    // -------------------------------------------------------------------------

    /**
     * 테스트 전용 Bean 설정. {@link LockTestService}를 Spring 컨텍스트에 등록하여
     * AOP 프록시가 @DistributedLock을 인터셉트할 수 있게 한다.
     *
     * @author kim-young-woong
     * @since 1.0.0
     */
    @TestConfiguration
    static class LockTestConfig {

        /**
         * @DistributedLock 어노테이션이 붙은 메서드를 가지는 테스트용 서비스 Bean.
         *
         * @return LockTestService 인스턴스
         */
        @Bean
        public LockTestService lockTestService() {
            return new LockTestService();
        }
    }

    /**
     * 분산 락 통합 테스트에서 사용하는 헬퍼 서비스.
     * 각 메서드는 AOP 프록시를 통해 @DistributedLock을 적용받는다.
     *
     * @author kim-young-woong
     * @since 1.0.0
     */
    static class LockTestService {

        /**
         * 기본 waitTime/leaseTime으로 락을 획득하고 작업을 완료한다.
         *
         * @param key 락 키 일부
         * @return "done:{key}" 형태의 결과 문자열
         */
        @DistributedLock(key = "'test:' + #key")
        public String doWork(String key) {
            return "done:" + key;
        }

        /**
         * waitTime이 100ms로 짧아 경합 시 빠르게 실패한다.
         *
         * @param key 락 키 일부
         * @return "done:{key}" 형태의 결과 문자열
         */
        @DistributedLock(key = "'test:' + #key", waitTime = 100, leaseTime = 5000)
        public String doWorkShortTimeout(String key) {
            return "done:" + key;
        }

        /**
         * leaseTime(300ms)보다 오래 걸리는 작업을 수행하여 leaseTime 만료를 유도한다.
         *
         * @param key 락 키 일부
         * @throws InterruptedException sleep 중단 시
         */
        @DistributedLock(key = "'test:' + #key", leaseTime = 300)
        public void doSlowWork(String key) throws InterruptedException {
            Thread.sleep(600); // leaseTime(300ms)보다 오래 걸림
        }

        /**
         * waitTime=100ms이지만 내부에서 300ms 슬립하여 경합 시 다른 스레드가 타임아웃되도록 유도한다.
         *
         * @param key 락 키 일부
         * @return "done:{key}" 형태의 결과 문자열
         * @throws InterruptedException sleep 중단 시
         */
        @DistributedLock(key = "'test:' + #key", waitTime = 100, leaseTime = 5000)
        public String doWorkSlowShortTimeout(String key) throws InterruptedException {
            Thread.sleep(300); // waitTime(100ms)보다 오래 보유하여 경합 유발
            return "done:" + key;
        }
    }

    // -------------------------------------------------------------------------
    // TC-1
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("@DistributedLock 어노테이션이 붙은 메서드가 AOP를 통해 정상 실행된다")
    void distributedLock_normalExecution_returnsExpectedResult() {
        // when
        String result = lockTestService.doWork("myKey");

        // then
        assertThat(result).isEqualTo("done:myKey");
    }

    // -------------------------------------------------------------------------
    // TC-2
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("다른 스레드가 락을 점유 중일 때 waitTime 초과 시 LockAcquisitionFailedException이 발생한다")
    void distributedLock_lockHeldByOtherThread_throwsLockAcquisitionFailedException() throws Exception {
        // given — Thread-A가 Redis 락 직접 점유
        String lockKey = "lock:test:sameKey";
        org.redisson.api.RLock rLock = redissonClient.getLock(lockKey);

        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch testDoneLatch = new CountDownLatch(1);

        Thread threadA = new Thread(() -> {
            rLock.lock();
            lockAcquiredLatch.countDown(); // Thread-A 락 점유 완료 신호
            try {
                testDoneLatch.await(5, TimeUnit.SECONDS); // 테스트 완료까지 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rLock.unlock();
            }
        });
        threadA.start();

        // Thread-A가 락을 점유할 때까지 대기
        lockAcquiredLatch.await(5, TimeUnit.SECONDS);

        try {
            // when & then — waitTime=100ms 초과로 실패
            assertThatThrownBy(() -> lockTestService.doWorkShortTimeout("sameKey"))
                    .isInstanceOf(LockAcquisitionFailedException.class);
        } finally {
            // finally — Thread-A 락 해제
            testDoneLatch.countDown();
            threadA.join(3000);
        }
    }

    // -------------------------------------------------------------------------
    // TC-3
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("서로 다른 락 키는 독립적으로 동시 획득 가능하다")
    void distributedLock_differentKeys_canBeAcquiredConcurrently() throws Exception {
        // given
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<String> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Throwable> errors = new java.util.concurrent.CopyOnWriteArrayList<>();

        Thread threadA = new Thread(() -> {
            try {
                startLatch.await();
                results.add(lockTestService.doWork("keyA"));
            } catch (Exception e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                startLatch.await();
                results.add(lockTestService.doWork("keyB"));
            } catch (Exception e) {
                errors.add(e);
            } finally {
                doneLatch.countDown();
            }
        });

        threadA.start();
        threadB.start();

        // when — 동시 시작
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        // then
        assertThat(errors).isEmpty();
        assertThat(results).hasSize(2);
        assertThat(results).contains("done:keyA", "done:keyB");
    }

    // -------------------------------------------------------------------------
    // TC-4
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("leaseTime 만료 시 락이 자동 해제되어 다른 스레드가 획득할 수 있다")
    void distributedLock_leaseTimeExpires_lockAutoReleasedAndReacquirable() throws Exception {
        // given — Thread-A가 doSlowWork 실행 (leaseTime=300ms, sleep=600ms → leaseTime 초과)
        Thread threadA = new Thread(() -> {
            try {
                lockTestService.doSlowWork("expKey");
            } catch (Exception e) {
                // leaseTime 만료 후 isHeldByCurrentThread()=false이므로 unlock 시도 없이 종료
                // 또는 InterruptedException — 무시
            }
        });
        threadA.start();

        // Thread-A가 락을 획득하고 작업을 시작할 충분한 시간 대기
        Thread.sleep(100);

        // leaseTime(300ms)이 만료될 충분한 시간 대기 (Thread-A 시작 후 400ms = leaseTime 초과)
        Thread.sleep(300);

        // when — leaseTime 만료되어 락이 자동 해제된 상태에서 다른 호출 시도
        String result = lockTestService.doWork("expKey");

        // then
        assertThat(result).isEqualTo("done:expKey");

        threadA.join(5000);
    }

    // -------------------------------------------------------------------------
    // TC-5
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("메서드 완료 후 Redis에 락 키가 잔존하지 않는다")
    void distributedLock_afterCompletion_lockKeyNotRemainingInRedis() {
        // given
        lockTestService.doWork("cleanupKey");

        // when
        boolean isLocked = redissonClient.getLock("lock:test:cleanupKey").isLocked();

        // then
        assertThat(isLocked).isFalse();
    }

    // -------------------------------------------------------------------------
    // TC-6
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("waitTime이 짧을 때 동시 요청 중 일부는 LockAcquisitionFailedException으로 실패한다")
    void distributedLock_shortWaitTime_someConcurrentRequestsFail() throws Exception {
        // given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<Void> future = executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작 대기
                    lockTestService.doWorkSlowShortTimeout("sharedKey");
                    successCount.incrementAndGet();
                } catch (LockAcquisitionFailedException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
                return null;
            });
            futures.add(future);
        }

        // when — 5개 스레드 동시 시작
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isGreaterThan(0);
        assertThat(successCount.get()).isGreaterThan(0);
    }
}
