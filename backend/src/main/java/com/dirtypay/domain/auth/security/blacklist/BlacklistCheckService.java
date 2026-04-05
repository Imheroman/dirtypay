package com.dirtypay.domain.auth.security.blacklist;

/**
 * JWT Access Token 블랙리스트 확인 서비스 인터페이스.
 *
 * <p>Spring Profile에 따라 3가지 구현체 중 하나가 활성화된다:</p>
 * <ul>
 *   <li>{@code NoOpBlacklistCheckService} — 블랙리스트 없음 (기본, Baseline)</li>
 *   <li>{@code DbBlacklistCheckService} — MariaDB 기반 조회</li>
 *   <li>{@code RedisBlacklistCheckService} — Redis 기반 조회</li>
 * </ul>
 *
 * <p>전략 패턴(Strategy Pattern)으로 런타임에 구현체를 교체할 수 있으며,
 * 부하 테스트 시 각 구현체의 성능 오버헤드를 비교한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface BlacklistCheckService {

    /**
     * 주어진 JWT ID(jti)가 블랙리스트에 등록되어 있는지 확인한다.
     *
     * @param jti JWT ID (UUID 문자열)
     * @return 블랙리스트 등록 여부
     */
    boolean isBlacklisted(String jti);
}
