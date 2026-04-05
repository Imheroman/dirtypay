package com.dirtypay.domain.auth.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import com.dirtypay.domain.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 제공자.
 *
 * <p>JWT 토큰의 생성, 검증, 파싱을 담당한다.
 * Access Token과 Refresh Token을 생성하며, HMAC-SHA 알고리즘으로 서명한다.</p>
 *
 * <p>Access Token에는 UUID 기반의 jti(JWT ID) claim이 포함되어 있어
 * 토큰 블랙리스트 조회 시 고유 식별자로 사용된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    /**
     * 빈 초기화 시 JWT 서명 키를 생성한다.
     *
     * <p>Spring 싱글톤 빈의 멀티스레드 환경에서 동시성 이슈를 방지하기 위해
     * lazy init 대신 eager init으로 1회 초기화한다.</p>
     */
    @PostConstruct
    private void initSigningKey() {
        this.signingKey = Keys.hmacShaKeyFor(
                this.jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 서명에 사용할 SecretKey를 반환한다.
     *
     * @return HMAC-SHA 알고리즘용 SecretKey
     */
    private SecretKey getSigningKey() {
        return this.signingKey;
    }

    /**
     * Access Token을 생성한다.
     *
     * <p>생성되는 토큰에는 UUID 기반의 jti(JWT ID) claim이 포함되어
     * 블랙리스트 조회 시 고유 식별자로 사용된다.</p>
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @param role     회원 권한 (예: "ROLE_USER", "ROLE_ADMIN")
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(Long memberId, String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + this.jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("memberId", memberId)
                .claim("role", role)
                .claim("type", "access")
                .issuer(this.jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * Refresh Token을 생성한다.
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @return 생성된 Refresh Token 문자열
     */
    public String createRefreshToken(Long memberId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + this.jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(email)
                .claim("memberId", memberId)
                .claim("type", "refresh")
                .issuer(this.jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(this.getSigningKey())
                .compact();
    }

    /**
     * 토큰의 유효성을 검증한다.
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 토큰이면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(this.getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token의 유효성을 검증한다.
     *
     * <p>토큰의 서명/만료 검증과 type claim이 "access"인지 확인한다.
     * Refresh Token이 Access Token으로 사용되는 것을 방지한다.</p>
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 Access Token이면 true, 그렇지 않으면 false
     */
    public boolean validateAccessToken(String token) {
        return this.validateToken(token) && this.isAccessToken(token);
    }

    /**
     * 토큰이 Access Token인지 확인한다.
     *
     * @param token JWT 토큰
     * @return type claim이 "access"이면 true
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = this.getClaims(token);
            return "access".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰에서 인증 정보를 추출하여 Authentication 객체를 생성한다.
     *
     * <p>DB 조회 없이 JWT Claims에서 직접 UserPrincipal을 생성한다.</p>
     *
     * @param token JWT 토큰
     * @return Spring Security Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = this.getClaims(token);
        Long memberId = claims.get("memberId", Long.class);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        UserPrincipal principal = UserPrincipal.fromClaims(memberId, email, role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    /**
     * 토큰에서 이메일을 추출한다.
     *
     * @param token JWT 토큰
     * @return 이메일
     */
    public String getEmail(String token) {
        return this.getClaims(token).getSubject();
    }

    /**
     * 토큰에서 회원 ID를 추출한다.
     *
     * @param token JWT 토큰
     * @return 회원 ID
     */
    public Long getMemberId(String token) {
        return this.getClaims(token).get("memberId", Long.class);
    }

    /**
     * 토큰에서 jti(JWT ID) claim을 추출한다.
     *
     * <p>jti는 Access Token 생성 시 UUID.randomUUID()로 발급되며,
     * 블랙리스트 조회의 고유 식별자로 사용된다.</p>
     *
     * @param token JWT 토큰
     * @return jti 값 (UUID 문자열), jti가 없으면 null
     */
    public String extractJti(String token) {
        return this.getClaims(token).getId();
    }

    /**
     * 만료된 토큰을 포함하여 안전하게 jti(JWT ID) claim을 추출한다.
     *
     * <p>서명이 유효한 토큰이라면 만료 여부와 관계없이 jti를 반환한다.
     * 로그아웃 시 이미 만료된 Access Token에서도 jti를 추출해 블랙리스트에 등록하는 용도로 사용한다.</p>
     *
     * <p>토큰이 만료된 경우 {@link ExpiredJwtException#getClaims()}에서 jti를 추출한다.
     * 서명 검증 실패, 파싱 불가 등 복구 불가능한 오류는 null을 반환한다.</p>
     *
     * @param token JWT 토큰 (만료된 토큰도 허용)
     * @return jti 값 (UUID 문자열), jti가 없거나 파싱 불가능한 경우 null
     */
    public String extractJtiSafely(String token) {
        try {
            return this.getClaims(token).getId();
        } catch (ExpiredJwtException e) {
            log.debug("Extracting jti from expired token for blacklist registration.");
            return e.getClaims().getId();
        } catch (Exception e) {
            log.warn("Failed to extract jti from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰의 남은 유효 시간을 밀리초 단위로 반환한다.
     *
     * <p>블랙리스트 TTL 설정 시 Access Token 만료 시각까지의 잔여 시간을 계산하는 데 사용한다.
     * 이미 만료된 토큰은 0을 반환한다. 만료로 인해 {@link ExpiredJwtException}이 발생하는 경우에도
     * 예외를 전파하지 않고 0을 반환한다.</p>
     *
     * @param token JWT 토큰 (만료된 토큰도 허용)
     * @return 남은 유효 시간 (밀리초), 만료된 경우 또는 파싱 불가능한 경우 0
     */
    public long getRemainingExpiryMillis(String token) {
        try {
            Date expiration = this.getClaims(token).getExpiration();
            return Math.max(0L, expiration.getTime() - System.currentTimeMillis());
        } catch (ExpiredJwtException e) {
            return 0L;
        }
    }

    /**
     * 토큰에서 Claims를 파싱한다.
     *
     * @param token JWT 토큰
     * @return 토큰의 Claims
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
