package com.dirtypay.domain.auth.security.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * DB 기반 블랙리스트 확인 서비스 (Profile: blacklist-db).
 *
 * <p>매 요청마다 MariaDB {@code token_blacklist} 테이블을 조회하여
 * jti의 등록 여부를 확인한다. 부하 테스트에서 DB 기반 블랙리스트의
 * 성능 오버헤드를 측정하기 위해 사용한다.</p>
 *
 * <p>HikariCP 커넥션을 인증 경로에서 추가로 점유하므로,
 * 고부하 시 커넥션 풀 경합이 발생할 수 있다. 이것이 Redis 대비
 * DB 구현의 핵심 차이점이다.</p>
 *
 * <p>테이블 구조:</p>
 * <pre>
 * CREATE TABLE token_blacklist (
 *     id         BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     jti        VARCHAR(36) NOT NULL COMMENT 'JWT ID (UUID)',
 *     expires_at DATETIME(3) NOT NULL,
 *     created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 *     UNIQUE KEY uk_jti (jti),
 *     KEY idx_expires_at (expires_at)
 * )
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Service
@Profile("blacklist-db")
@RequiredArgsConstructor
public class DbBlacklistCheckService implements BlacklistCheckService {

    private static final String IS_BLACKLISTED_SQL =
            "SELECT COUNT(*) FROM token_blacklist WHERE jti = ? AND expires_at > NOW()";

    private final JdbcTemplate jdbcTemplate;

    /**
     * {@code token_blacklist} 테이블에서 jti를 조회하여 블랙리스트 등록 여부를 확인한다.
     *
     * <p>UNIQUE INDEX({@code uk_jti})를 통해 O(1) 조회를 보장한다.
     * 만료된 토큰({@code expires_at <= NOW()})은 블랙리스트에 있더라도 MISS로 처리한다.</p>
     *
     * @param jti JWT ID
     * @return 블랙리스트에 등록된 경우 true
     */
    @Override
    public boolean isBlacklisted(String jti) {
        Integer count = jdbcTemplate.queryForObject(IS_BLACKLISTED_SQL, Integer.class, jti);
        boolean result = count != null && count > 0;
        log.debug("Blacklist check (DB): jti={} result={}", jti, result);
        return result;
    }
}
