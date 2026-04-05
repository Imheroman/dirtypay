package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.domain.auth.security.UserPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * JWT 토큰 제공자 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    // 테스트용 최소 256비트(32바이트) 비밀키
    private static final String TEST_SECRET_KEY =
            "test-secret-key-for-unit-testing-minimum-256-bits!!";
    private static final long ACCESS_TOKEN_EXPIRATION = 3_600_000L;   // 1시간 (ms)
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L; // 7일 (ms)
    private static final String ISSUER = "dirtypay";
    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "test@dirtypay.com";

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // lenient: 일부 테스트에서 사용되지 않는 스텁도 예외 없이 허용
        // (각 테스트마다 필요한 스텁 조합이 다르기 때문)
        lenient().when(jwtProperties.getSecretKey()).thenReturn(TEST_SECRET_KEY);
        lenient().when(jwtProperties.getIssuer()).thenReturn(ISSUER);
        lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        // @PostConstruct는 Mockito @InjectMocks에서 자동 실행되지 않으므로 수동 초기화
        ReflectionTestUtils.invokeMethod(jwtTokenProvider, "initSigningKey");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Access Token 생성 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createAccessToken 테스트")
    class CreateAccessTokenTest {

        @Test
        @DisplayName("Access Token 생성 시 토큰 문자열이 반환된다")
        void createAccessToken_returnsNonEmptyToken() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // then
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Access Token의 subject(email) Claim이 올바르게 설정된다")
        void createAccessToken_emailClaimIsCorrect() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // then
            String email = parseClaims(token).getSubject();
            assertThat(email).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Access Token의 memberId Claim이 올바르게 설정된다")
        void createAccessToken_memberIdClaimIsCorrect() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // then
            // jjwt는 JSON 숫자를 Integer로 역직렬화하므로 longValue()로 변환
            Number memberId = parseClaims(token).get("memberId", Number.class);
            assertThat(memberId.longValue()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("Access Token의 type Claim이 'access'로 설정된다")
        void createAccessToken_typeClaimIsAccess() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // then
            String type = parseClaims(token).get("type", String.class);
            assertThat(type).isEqualTo("access");
        }

        @Test
        @DisplayName("Access Token의 role Claim이 올바르게 설정된다")
        void createAccessToken_roleClaimIsCorrect() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // then
            String role = parseClaims(token).get("role", String.class);
            assertThat(role).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("ADMIN role로 생성한 Access Token의 role Claim이 올바르게 설정된다")
        void createAccessToken_adminRoleClaimIsCorrect() {
            // when
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_ADMIN");

            // then
            String role = parseClaims(token).get("role", String.class);
            assertThat(role).isEqualTo("ROLE_ADMIN");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh Token 생성 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createRefreshToken 테스트")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("Refresh Token 생성 시 토큰 문자열이 반환된다")
        void createRefreshToken_returnsNonEmptyToken() {
            // when
            String token = jwtTokenProvider.createRefreshToken(MEMBER_ID, EMAIL);

            // then
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Refresh Token의 type Claim이 'refresh'로 설정된다")
        void createRefreshToken_typeClaimIsRefresh() {
            // when
            String token = jwtTokenProvider.createRefreshToken(MEMBER_ID, EMAIL);

            // then
            String type = parseClaims(token).get("type", String.class);
            assertThat(type).isEqualTo("refresh");
        }

        @Test
        @DisplayName("Refresh Token의 subject(email)과 memberId Claim이 올바르게 설정된다")
        void createRefreshToken_subjectAndMemberIdAreCorrect() {
            // when
            String token = jwtTokenProvider.createRefreshToken(MEMBER_ID, EMAIL);

            // then
            var claims = parseClaims(token);
            assertThat(claims.getSubject()).isEqualTo(EMAIL);
            Number memberId = claims.get("memberId", Number.class);
            assertThat(memberId.longValue()).isEqualTo(MEMBER_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 토큰 유효성 검증 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken 테스트")
    class ValidateTokenTest {

        @Test
        @DisplayName("정상적으로 발급된 토큰은 true를 반환한다")
        void validateToken_validToken_returnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            boolean result = jwtTokenProvider.validateToken(token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void validateToken_expiredToken_returnsFalse() {
            // given — 이미 만료된 토큰 직접 생성 (expiration = -1초)
            String expiredToken = buildTokenWithCustomExpiration(-1000L);

            // when
            boolean result = jwtTokenProvider.validateToken(expiredToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("변조된 서명을 가진 토큰은 false를 반환한다")
        void validateToken_tamperedToken_returnsFalse() {
            // given — 유효한 토큰의 마지막 서명 부분을 임의로 변조
            String validToken = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");
            String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "invalidsignature";

            // when
            boolean result = jwtTokenProvider.validateToken(tamperedToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰은 false를 반환한다")
        void validateToken_emptyString_returnsFalse() {
            // when
            boolean result = jwtTokenProvider.validateToken("");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 토큰은 false를 반환한다")
        void validateToken_null_returnsFalse() {
            // when
            boolean result = jwtTokenProvider.validateToken(null);

            // then
            assertThat(result).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Access Token 유효성 검증 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateAccessToken 테스트")
    class ValidateAccessTokenTest {

        @Test
        @DisplayName("유효한 Access Token은 true를 반환한다")
        void validateAccessToken_validAccessToken_returnsTrue() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            boolean result = jwtTokenProvider.validateAccessToken(token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Refresh Token은 false를 반환한다")
        void validateAccessToken_refreshToken_returnsFalse() {
            // given
            String token = jwtTokenProvider.createRefreshToken(MEMBER_ID, EMAIL);

            // when
            boolean result = jwtTokenProvider.validateAccessToken(token);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("만료된 Access Token은 false를 반환한다")
        void validateAccessToken_expiredAccessToken_returnsFalse() {
            // given
            String expiredToken = buildTokenWithCustomExpiration(-1000L);

            // when
            boolean result = jwtTokenProvider.validateAccessToken(expiredToken);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("type claim이 없는 토큰은 false를 반환한다")
        void validateAccessToken_noTypeClaim_returnsFalse() {
            // given — type claim 없이 토큰 생성
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Date now = new Date();
            String token = Jwts.builder()
                    .subject(EMAIL)
                    .claim("memberId", MEMBER_ID)
                    .issuer(ISSUER)
                    .issuedAt(now)
                    .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION))
                    .signWith(key)
                    .compact();

            // when
            boolean result = jwtTokenProvider.validateAccessToken(token);

            // then
            assertThat(result).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authentication 추출 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAuthentication 테스트")
    class GetAuthenticationTest {

        @Test
        @DisplayName("유효한 토큰에서 Authentication 객체가 반환된다")
        void getAuthentication_validToken_returnsAuthentication() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // then
            assertThat(authentication).isNotNull();
            assertThat(authentication.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("Authentication의 Principal이 UserPrincipal 타입이고 email이 올바르다")
        void getAuthentication_validToken_principalIsUserPrincipal() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // then
            assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            assertThat(principal.getEmail()).isEqualTo(EMAIL);
            assertThat(principal.getId()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("Authentication에 ROLE_USER 권한이 설정된다")
        void getAuthentication_validToken_hasRoleUser() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            // then
            assertThat(authentication.getAuthorities())
                    .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Claim 추출 메서드 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getEmail 테스트")
    class GetEmailTest {

        @Test
        @DisplayName("토큰에서 이메일을 추출한다")
        void getEmail_validToken_returnsEmail() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            String extractedEmail = jwtTokenProvider.getEmail(token);

            // then
            assertThat(extractedEmail).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("getMemberId 테스트")
    class GetMemberIdTest {

        @Test
        @DisplayName("토큰에서 회원 ID를 Long 타입으로 추출한다")
        void getMemberId_validToken_returnsMemberIdAsLong() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            Long extractedMemberId = jwtTokenProvider.getMemberId(token);

            // then
            assertThat(extractedMemberId).isEqualTo(MEMBER_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // extractJtiSafely 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractJtiSafely 테스트")
    class ExtractJtiSafelyTest {

        @Test
        @DisplayName("유효한 Access Token에서 jti를 추출한다")
        void extractJtiSafely_validToken_returnsJti() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            String jti = jwtTokenProvider.extractJtiSafely(token);

            // then
            assertThat(jti).isNotBlank();
        }

        @Test
        @DisplayName("만료된 Access Token에서도 jti를 추출한다 — ExpiredJwtException 전파 없음")
        void extractJtiSafely_expiredToken_returnsJtiWithoutException() {
            // given — jti가 포함된 만료 토큰 생성
            String expiredToken = buildExpiredAccessTokenWithJti();

            // when — ExpiredJwtException이 전파되지 않아야 한다
            String jti = jwtTokenProvider.extractJtiSafely(expiredToken);

            // then
            assertThat(jti).isNotBlank();
        }

        @Test
        @DisplayName("서명이 변조된 토큰은 null을 반환한다")
        void extractJtiSafely_tamperedToken_returnsNull() {
            // given
            String validToken = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");
            String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "invalidsignature";

            // when
            String jti = jwtTokenProvider.extractJtiSafely(tamperedToken);

            // then
            assertThat(jti).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRemainingExpiryMillis 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRemainingExpiryMillis 테스트")
    class GetRemainingExpiryMillisTest {

        @Test
        @DisplayName("유효한 토큰은 남은 유효 시간(양수)을 반환한다")
        void getRemainingExpiryMillis_validToken_returnsPositive() {
            // given
            String token = jwtTokenProvider.createAccessToken(MEMBER_ID, EMAIL, "ROLE_USER");

            // when
            long remaining = jwtTokenProvider.getRemainingExpiryMillis(token);

            // then
            assertThat(remaining).isGreaterThan(0L);
        }

        @Test
        @DisplayName("만료된 토큰은 0을 반환한다 — ExpiredJwtException 전파 없음")
        void getRemainingExpiryMillis_expiredToken_returnsZeroWithoutException() {
            // given
            String expiredToken = buildTokenWithCustomExpiration(-1000L);

            // when — ExpiredJwtException이 전파되지 않아야 한다
            long remaining = jwtTokenProvider.getRemainingExpiryMillis(expiredToken);

            // then
            assertThat(remaining).isEqualTo(0L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 테스트용 비밀키로 Claims를 파싱한다.
     */
    private io.jsonwebtoken.Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 만료 시간을 직접 지정하여 토큰을 생성한다 (만료 토큰 시나리오용).
     *
     * @param expirationOffset 현재 시각 기준 만료 오프셋 (ms, 음수면 이미 만료)
     * @return 생성된 JWT 토큰
     */
    private String buildTokenWithCustomExpiration(long expirationOffset) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(EMAIL)
                .claim("memberId", MEMBER_ID)
                .claim("type", "access")
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationOffset))
                .signWith(key)
                .compact();
    }

    /**
     * jti claim이 포함된 만료 Access Token을 생성한다 (extractJtiSafely 시나리오용).
     *
     * @return jti claim을 포함한 만료된 JWT 토큰
     */
    private String buildExpiredAccessTokenWithJti() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(EMAIL)
                .claim("memberId", MEMBER_ID)
                .claim("type", "access")
                .issuer(ISSUER)
                .issuedAt(now)
                .expiration(new Date(now.getTime() - 1000L))
                .signWith(key)
                .compact();
    }

}
