package com.dirtypay.global.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분산 락 내 트랜잭션 분리 헬퍼.
 *
 * <p>락 해제 전에 트랜잭션이 COMMIT되도록 {@code REQUIRES_NEW}로 실행한다.
 * Aspect 내에서 직접 호출 시 프록시 체인이 우회되므로 별도 Bean으로 분리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
public class AopForTransaction {

    /**
     * 조인 포인트를 REQUIRES_NEW 트랜잭션으로 실행한다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메서드 반환값
     * @throws Throwable 원본 메서드에서 발생한 예외
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
