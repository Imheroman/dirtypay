package com.dirtypay.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT Access Token 블랙리스트 서비스.
 *
 * <p>로그아웃된 Access Token을 Redis에 등록하고 매 요청마다 조회한다.</p>
 *
 * <p>키 네임스페이스: {@code dirtypay:blacklist:{jti}}</p>
 *
 * <p>Redis 장애 시 Fail-Closed 전략 적용 — 결제 서비스 특성상 보안 우선.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "dirtypay:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 로그아웃 시 Access Token을 블랙리스트에 등록한다.
     *
     * @param jti             토큰의 jti 클레임 (UUID)
     * @param remainingMillis 토큰 잔여 만료 시간 (밀리초)
     */
    public void blacklistAccessToken(String jti, long remainingMillis) {
        if (remainingMillis <= 0) {
            log.debug("Token already expired, skip blacklist registration: jti={}", jti);
            return;
        }
        this.stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "revoked",
                remainingMillis,
                TimeUnit.MILLISECONDS
        );
        log.info("Access token blacklisted: jti={}, ttl={}ms", jti, remainingMillis);
    }

    /**
     * 매 요청에서 토큰이 블랙리스트에 등록되어 있는지 확인한다.
     *
     * <p>Redis 연결 실패 시 Fail-Closed: {@code true}를 반환하여 접근을 차단한다.</p>
     *
     * @param jti 토큰의 jti 클레임 (UUID)
     * @return 블랙리스트 등록 여부
     */
    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(
                    this.stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti)
            );
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed during blacklist check, applying Fail-Closed: jti={}", jti, e);
            return true;
        }
    }
}
