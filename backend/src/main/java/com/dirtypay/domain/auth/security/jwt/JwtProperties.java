package com.dirtypay.domain.auth.security.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 *
 * <p>application.yml의 jwt 프로퍼티를 바인딩한다.
 * 토큰 생성 및 검증에 필요한 설정값을 제공한다.</p>
 *
 * <p>보안 민감 설정(secretKey, 만료 시간 등)의 런타임 변경을 방지하기 위해
 * 생성자 바인딩 방식으로 불변 객체로 관리한다.</p>
 *
 * <pre>
 * jwt:
 *   secret-key: ${JWT_SECRET_KEY:...}
 *   access-token-expiration: 3600000
 *   refresh-token-expiration: 604800000
 *   issuer: dirtypay
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 서명에 사용되는 비밀키 */
    private final String secretKey;

    /**
     * Access Token 만료 시간 (밀리초).
     *
     * <p>Access Token은 폐기(Revoke)가 불가능하므로,
     * 만료시간을 짧게(5~15분) 유지하여 리스크를 완화해야 한다.</p>
     */
    private final long accessTokenExpiration;

    /** Refresh Token 만료 시간 (밀리초) */
    private final long refreshTokenExpiration;

    /** 토큰 발급자 */
    private final String issuer;
}
