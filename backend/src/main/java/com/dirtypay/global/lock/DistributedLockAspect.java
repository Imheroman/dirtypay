package com.dirtypay.global.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 분산 락 AOP Aspect.
 *
 * <p>{@link DistributedLock} 어노테이션이 붙은 메서드 실행 전후로 Redisson 분산 락을 관리한다.
 * 락 획득 후 {@link AopForTransaction}을 통해 REQUIRES_NEW 트랜잭션으로 원본 메서드를 실행하여
 * 트랜잭션 커밋 후 락이 해제되는 순서를 보장한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private static final String LOCK_PREFIX = "lock:";

    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    /**
     * 분산 락을 획득하고 원본 메서드를 실행한다.
     *
     * @param joinPoint       AOP 조인 포인트
     * @param distributedLock 어노테이션 메타데이터
     * @return 원본 메서드 반환값
     * @throws Throwable 락 획득 실패 또는 원본 메서드 예외
     */
    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String parsedKey = CustomSpelExpressionParser.parse(distributedLock.key(), joinPoint);
        String lockKey = LOCK_PREFIX + parsedKey;

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit());

            if (!acquired) {
                log.warn("[분산 락] 획득 실패: key={}", lockKey);
                throw new LockAcquisitionFailedException(lockKey);
            }

            log.debug("[분산 락] 획득: key={}", lockKey);
            return aopForTransaction.proceed(joinPoint);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("[분산 락] 해제: key={}", lockKey);
            }
        }
    }
}
