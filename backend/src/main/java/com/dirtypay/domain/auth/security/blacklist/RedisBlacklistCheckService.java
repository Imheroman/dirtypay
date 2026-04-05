package com.dirtypay.domain.auth.security.blacklist;

import com.dirtypay.domain.auth.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 블랙리스트 확인 서비스 (Profile: blacklist-redis).
 *
 * <p>매 요청마다 Redis에서 jti를 조회하여 블랙리스트 등록 여부를 확인한다.
 * {@link TokenBlacklistService}에 위임하여 실제 Redis 조회를 수행한다.</p>
 *
 * <p>Redis 장애 시 {@link TokenBlacklistService#isBlacklisted(String)}의
 * Fail-Closed 전략이 적용된다 — {@code true} 반환으로 접근 차단.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Service
@Profile("blacklist-redis")
@RequiredArgsConstructor
public class RedisBlacklistCheckService implements BlacklistCheckService {

    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Redis에서 jti를 조회하여 블랙리스트 등록 여부를 확인한다.
     *
     * @param jti JWT ID (UUID 문자열)
     * @return 블랙리스트 등록 여부
     */
    @Override
    public boolean isBlacklisted(String jti) {
        boolean result = this.tokenBlacklistService.isBlacklisted(jti);
        log.debug("Blacklist check (Redis): jti={}, result={}", jti, result);
        return result;
    }
}
