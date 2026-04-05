package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.domain.auth.security.CookieUtil;
import com.dirtypay.domain.auth.security.blacklist.BlacklistCheckService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * JWT 인증 필터 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String COOKIE_TOKEN = "cookie.jwt.token";

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private BlacklistCheckService blacklistCheckService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        // 각 테스트 전 SecurityContext를 초기화하여 테스트 간 독립성 보장
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bearer 토큰 처리 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Bearer 토큰 처리 테스트")
    class BearerTokenTest {

        @Test
        @DisplayName("유효한 Bearer 토큰이 있으면 SecurityContext에 인증 정보가 설정된다")
        void doFilterInternal_validBearerToken_setsAuthentication() throws ServletException, IOException {
            // given
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            Authentication authentication = buildAuthentication();

            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getAuthentication(VALID_TOKEN)).willReturn(authentication);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("test@dirtypay.com");
        }

        @Test
        @DisplayName("유효한 Bearer 토큰 처리 후 filterChain.doFilter()가 반드시 호출된다")
        void doFilterInternal_validBearerToken_filterChainCalled() throws ServletException, IOException {
            // given
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            Authentication authentication = buildAuthentication();

            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getAuthentication(VALID_TOKEN)).willReturn(authentication);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("유효하지 않은 Bearer 토큰이면 SecurityContext에 인증 정보가 설정되지 않는다")
        void doFilterInternal_invalidBearerToken_doesNotSetAuthentication() throws ServletException, IOException {
            // given
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + INVALID_TOKEN);

            given(jwtTokenProvider.validateAccessToken(INVALID_TOKEN)).willReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            // 유효하지 않은 토큰이므로 getAuthentication()은 호출되지 않아야 한다
            verify(jwtTokenProvider, never()).getAuthentication(INVALID_TOKEN);
        }

        @Test
        @DisplayName("유효하지 않은 Bearer 토큰이어도 filterChain.doFilter()는 반드시 호출된다")
        void doFilterInternal_invalidBearerToken_filterChainStillCalled() throws ServletException, IOException {
            // given
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + INVALID_TOKEN);

            given(jwtTokenProvider.validateAccessToken(INVALID_TOKEN)).willReturn(false);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cookie 토큰 처리 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cookie 토큰 처리 테스트")
    class CookieTokenTest {

        @Test
        @DisplayName("Authorization 헤더가 없을 때 유효한 Cookie 토큰이 있으면 인증 정보가 설정된다")
        void doFilterInternal_validCookieToken_setsAuthentication() throws ServletException, IOException {
            // given — Authorization 헤더 없음, Cookie에서 토큰 반환
            given(cookieUtil.extractAccessTokenFromCookie(request))
                    .willReturn(Optional.of(COOKIE_TOKEN));
            given(jwtTokenProvider.validateAccessToken(COOKIE_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getAuthentication(COOKIE_TOKEN)).willReturn(buildAuthentication());

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 토큰이 있으면 Cookie 토큰보다 우선하여 처리된다")
        void doFilterInternal_bearerTokenPriorityOverCookie() throws ServletException, IOException {
            // given — Authorization 헤더(Bearer)와 Cookie 모두 존재
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            Authentication authentication = buildAuthentication();

            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.getAuthentication(VALID_TOKEN)).willReturn(authentication);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            // Bearer 토큰이 사용되어야 하므로 Cookie 토큰으로 validate가 호출되지 않아야 한다
            verify(jwtTokenProvider, never()).validateAccessToken(COOKIE_TOKEN);
            verify(jwtTokenProvider).validateAccessToken(VALID_TOKEN);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 토큰 없음 처리 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("토큰 없음 처리 테스트")
    class NoTokenTest {

        @Test
        @DisplayName("Authorization 헤더도 Cookie도 없으면 SecurityContext에 인증 정보가 설정되지 않는다")
        void doFilterInternal_noToken_doesNotSetAuthentication() throws ServletException, IOException {
            // given — Authorization 헤더 없음, Cookie도 없음
            given(cookieUtil.extractAccessTokenFromCookie(request))
                    .willReturn(Optional.empty());

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            // 토큰이 없으므로 validateAccessToken()과 getAuthentication()은 호출되지 않아야 한다
            verify(jwtTokenProvider, never()).validateAccessToken(VALID_TOKEN);
            verify(jwtTokenProvider, never()).getAuthentication(VALID_TOKEN);
        }

        @Test
        @DisplayName("토큰이 없어도 filterChain.doFilter()는 반드시 호출된다")
        void doFilterInternal_noToken_filterChainStillCalled() throws ServletException, IOException {
            // given
            given(cookieUtil.extractAccessTokenFromCookie(request))
                    .willReturn(Optional.empty());

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 블랙리스트 처리 테스트
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블랙리스트 처리 테스트")
    class BlacklistCheckTest {

        @Test
        @DisplayName("블랙리스트에 등록된 jti이면 401을 반환하고 filterChain은 호출되지 않는다")
        void doFilterInternal_블랙리스트Hit_401반환() throws ServletException, IOException {
            // given
            String jti = "test-uuid";
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractJti(VALID_TOKEN)).willReturn(jti);
            given(blacklistCheckService.isBlacklisted(jti)).willReturn(true);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, never()).doFilter(any(), any());
            verify(jwtTokenProvider, never()).getAuthentication(anyString());
        }

        @Test
        @DisplayName("블랙리스트에 없는 jti이면 정상 인증 흐름이 진행된다")
        void doFilterInternal_블랙리스트Miss_정상인증() throws ServletException, IOException {
            // given
            String jti = "test-uuid";
            Authentication auth = buildAuthentication();
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractJti(VALID_TOKEN)).willReturn(jti);
            given(blacklistCheckService.isBlacklisted(jti)).willReturn(false);
            given(jwtTokenProvider.getAuthentication(VALID_TOKEN)).willReturn(auth);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("jti가 null이면 블랙리스트 확인을 skip하고 정상 인증 흐름이 진행된다")
        void doFilterInternal_jtiNull_블랙리스트확인Skip() throws ServletException, IOException {
            // given
            Authentication auth = buildAuthentication();
            request.addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + VALID_TOKEN);
            given(jwtTokenProvider.validateAccessToken(VALID_TOKEN)).willReturn(true);
            given(jwtTokenProvider.extractJti(VALID_TOKEN)).willReturn(null);
            given(jwtTokenProvider.getAuthentication(VALID_TOKEN)).willReturn(auth);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(blacklistCheckService, never()).isBlacklisted(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 헬퍼 메서드
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 테스트용 Authentication 객체를 생성한다.
     *
     * @return UsernamePasswordAuthenticationToken 기반 Authentication
     */
    private Authentication buildAuthentication() {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                User.builder()
                        .username("test@dirtypay.com")
                        .password("password")
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();

        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}
