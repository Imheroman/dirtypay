package com.dirtypay.domain.auth.security.blacklist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 블랙리스트 미구현 서비스 — 항상 MISS 반환 (Baseline).
 *
 * <p>blacklist-db, blacklist-redis 프로파일이 모두 비활성화된 경우 동작한다.
 * 기존 인증 흐름과 완전히 동일하며, 추가 I/O 없음.</p>
 *
 * <p>부하 테스트에서 BlackList 조회가 없는 순수 인증 경로의 성능 기준선(Baseline)
 * 측정에 사용된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@Profile("!blacklist-db & !blacklist-redis")
public class NoOpBlacklistCheckService implements BlacklistCheckService {

    /**
     * 항상 {@code false}를 반환한다 (블랙리스트 없음).
     *
     * <p>추가 I/O 없이 즉시 반환하므로 기존 인증 흐름과 성능이 동일하다.</p>
     *
     * @param jti JWT ID
     * @return 항상 false
     */
    @Override
    public boolean isBlacklisted(String jti) {
        return false;
    }
}
