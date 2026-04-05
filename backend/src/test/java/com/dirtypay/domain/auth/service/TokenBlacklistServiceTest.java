package com.dirtypay.domain.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * TokenBlacklistService 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private static final String JTI = "test-jti-uuid-1234";
    private static final String EXPECTED_KEY = "dirtypay:blacklist:" + JTI;

    @Test
    @DisplayName("blacklistAccessToken: 잔여 시간이 양수이면 Redis에 SET한다")
    void blacklistAccessToken_whenRemainingMillisPositive_setsToRedis() {
        // given
        long remainingMillis = 3000L;
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        tokenBlacklistService.blacklistAccessToken(JTI, remainingMillis);

        // then
        then(valueOperations).should()
                .set(EXPECTED_KEY, "revoked", remainingMillis, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("blacklistAccessToken: 잔여 시간이 0 이하이면 Redis SET을 호출하지 않는다")
    void blacklistAccessToken_whenRemainingMillisZeroOrNegative_doesNotSet() {
        // when
        tokenBlacklistService.blacklistAccessToken(JTI, 0L);
        tokenBlacklistService.blacklistAccessToken(JTI, -100L);

        // then
        then(valueOperations).should(never())
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("isBlacklisted: Redis에 키가 존재하면 true를 반환한다")
    void isBlacklisted_whenKeyExists_returnsTrue() {
        // given
        given(stringRedisTemplate.hasKey(EXPECTED_KEY)).willReturn(Boolean.TRUE);

        // when
        boolean result = tokenBlacklistService.isBlacklisted(JTI);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted: Redis에 키가 없으면 false를 반환한다")
    void isBlacklisted_whenKeyAbsent_returnsFalse() {
        // given
        given(stringRedisTemplate.hasKey(EXPECTED_KEY)).willReturn(Boolean.FALSE);

        // when
        boolean result = tokenBlacklistService.isBlacklisted(JTI);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted: Redis 연결 실패 시 Fail-Closed로 true를 반환한다")
    void isBlacklisted_whenRedisConnectionFails_returnsTrueFailClosed() {
        // given
        given(stringRedisTemplate.hasKey(EXPECTED_KEY))
                .willThrow(new RedisConnectionFailureException("Connection refused"));

        // when
        boolean result = tokenBlacklistService.isBlacklisted(JTI);

        // then
        assertThat(result).isTrue();
    }
}
