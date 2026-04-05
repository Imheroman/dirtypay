package com.dirtypay.global.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link DistributedLockAspect} 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DistributedLockAspect 단위 테스트")
class DistributedLockAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private AopForTransaction aopForTransaction;

    @Mock
    private DistributedLock distributedLock;

    @InjectMocks
    private DistributedLockAspect aspect;

    @Nested
    @DisplayName("around")
    class Around {

        @BeforeEach
        void setUpCommonMocks() {
            // 공통 JoinPoint / DistributedLock 기본값 설정
            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[]{"key"});
            given(joinPoint.getArgs()).willReturn(new Object[]{"testValue"});

            given(distributedLock.key()).willReturn("#key");
            given(distributedLock.waitTime()).willReturn(5000L);
            given(distributedLock.leaseTime()).willReturn(3000L);
            given(distributedLock.timeUnit()).willReturn(TimeUnit.MILLISECONDS);
        }

        @Test
        @DisplayName("tryLock 성공 시 aopForTransaction.proceed()가 호출되고 결과를 반환한다")
        void around_tryLockSucceeds_proceedCalledAndResultReturned() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(true);
            given(aopForTransaction.proceed(joinPoint)).willReturn("expected");

            // when
            Object result = aspect.around(joinPoint, distributedLock);

            // then
            assertThat(result).isEqualTo("expected");
            verify(aopForTransaction).proceed(joinPoint);
        }

        @Test
        @DisplayName("tryLock 실패(acquired=false) 시 LockAcquisitionFailedException을 던진다")
        void around_tryLockFails_throwsLockAcquisitionFailedException() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint, distributedLock))
                    .isInstanceOf(LockAcquisitionFailedException.class);
            verify(aopForTransaction, never()).proceed(any());
        }

        @Test
        @DisplayName("정상 완료 후 isHeldByCurrentThread()가 true이면 unlock()이 호출된다")
        void around_normalCompletion_unlockCalledWhenHeldByCurrentThread() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(true);
            given(aopForTransaction.proceed(joinPoint)).willReturn("result");
            given(lock.isHeldByCurrentThread()).willReturn(true);

            // when
            aspect.around(joinPoint, distributedLock);

            // then
            verify(lock).unlock();
        }

        @Test
        @DisplayName("proceed()에서 예외 발생 시에도 isHeldByCurrentThread()가 true이면 unlock()이 호출된다")
        void around_proceedThrowsException_unlockStillCalledWhenHeldByCurrentThread() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(true);
            given(aopForTransaction.proceed(joinPoint)).willThrow(new RuntimeException("오류"));
            given(lock.isHeldByCurrentThread()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint, distributedLock))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("오류");
            verify(lock).unlock();
        }

        @Test
        @DisplayName("isHeldByCurrentThread()가 false이면 unlock()을 호출하지 않는다")
        void around_proceedThrowsException_unlockNotCalledWhenNotHeldByCurrentThread() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(true);
            given(aopForTransaction.proceed(joinPoint)).willThrow(new RuntimeException("오류"));
            given(lock.isHeldByCurrentThread()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint, distributedLock))
                    .isInstanceOf(RuntimeException.class);
            verify(lock, never()).unlock();
        }

        @Test
        @DisplayName("lockKey는 'lock:' + SpEL 평가 결과로 생성된다")
        void around_lockKeyIsBuiltFromLockPrefixAndSpelResult() throws Throwable {
            // given
            // TC-6: key/args 를 Override
            given(distributedLock.key()).willReturn("'wallet:' + #receiverId");
            given(signature.getParameterNames()).willReturn(new String[]{"receiverId"});
            given(joinPoint.getArgs()).willReturn(new Object[]{42L});

            given(redissonClient.getLock("lock:wallet:42")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(true);
            given(aopForTransaction.proceed(joinPoint)).willReturn(null);
            given(lock.isHeldByCurrentThread()).willReturn(true);

            // when
            aspect.around(joinPoint, distributedLock);

            // then
            verify(redissonClient).getLock("lock:wallet:42");
        }

        @Test
        @DisplayName("락 획득 실패 시 finally에서 unlock()이 호출되지 않는다")
        void around_lockAcquisitionFails_unlockNeverCalled() throws Throwable {
            // given
            given(redissonClient.getLock("lock:testValue")).willReturn(lock);
            given(lock.tryLock(5000L, 3000L, TimeUnit.MILLISECONDS)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> aspect.around(joinPoint, distributedLock))
                    .isInstanceOf(LockAcquisitionFailedException.class);
            verify(lock, never()).unlock();
        }
    }
}
