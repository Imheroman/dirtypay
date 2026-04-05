package com.dirtypay.global.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락 어노테이션.
 * SpEL 표현식으로 락 키를 지정한다.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * SpEL 표현식으로 생성되는 락 키 (prefix "lock:"이 자동 추가됨).
     *
     * @return SpEL 표현식 문자열
     */
    String key();

    /**
     * 락 획득 최대 대기 시간.
     *
     * @return 대기 시간 (기본값: 5000ms)
     */
    long waitTime() default 5000L;

    /**
     * 락 유지 시간.
     *
     * <p>기본값 {@code -1}은 Redisson WatchDog 모드를 활성화한다.
     * WatchDog는 락 보유 중 자동으로 리스를 갱신하여
     * {@code waitTime} < {@code leaseTime} 역전으로 인한 자동 해제 위험을 제거한다.</p>
     *
     * @return 유지 시간 (기본값: -1 → WatchDog 자동 갱신)
     */
    long leaseTime() default -1L;

    /**
     * 시간 단위.
     *
     * @return TimeUnit (기본값: MILLISECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
