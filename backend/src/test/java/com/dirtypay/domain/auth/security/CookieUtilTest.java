package com.dirtypay.domain.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * CookieUtil 단위 테스트.
 *
 * <p>Access Token Cookie 생성, 삭제 쿠키 생성, 쿠키 추출 등의
 * 기능을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CookieUtilTest {

    @InjectMocks
    private CookieUtil cookieUtil;

    @Mock
    private CookieProperties cookieProperties;

    // 테스트 공통 상수
    private static final String ACCESS_TOKEN_NAME = "access_token";
    private static final String SAMPLE_TOKEN = "sample.jwt.token";
    private static final String COOKIE_PATH = "/api";
    private static final String SAME_SITE = "Lax";

    /**
     * 공통 CookieProperties stub 설정.
     */
    private void stubCookieProperties() {
        given(cookieProperties.getAccessTokenName()).willReturn(ACCESS_TOKEN_NAME);
        given(cookieProperties.isHttpOnly()).willReturn(true);
        given(cookieProperties.isSecure()).willReturn(false);
        given(cookieProperties.getSameSite()).willReturn(SAME_SITE);
        given(cookieProperties.getPath()).willReturn(COOKIE_PATH);
    }

    @Nested
    @DisplayName("createAccessTokenCookie 테스트")
    class CreateAccessTokenCookieTest {

        @Test
        @DisplayName("Access Token Cookie 생성 시 HttpOnly가 true로 설정된다")
        void createAccessTokenCookie_httpOnlyIsTrue() {
            // given
            stubCookieProperties();
            long maxAge = 3600L;

            // when
            ResponseCookie cookie = cookieUtil.createAccessTokenCookie(SAMPLE_TOKEN, maxAge);

            // then
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("Access Token Cookie 생성 시 path가 /api로 설정된다")
        void createAccessTokenCookie_pathIsApi() {
            // given
            stubCookieProperties();
            long maxAge = 3600L;

            // when
            ResponseCookie cookie = cookieUtil.createAccessTokenCookie(SAMPLE_TOKEN, maxAge);

            // then
            assertThat(cookie.getPath()).isEqualTo(COOKIE_PATH);
        }

        @Test
        @DisplayName("Access Token Cookie 생성 시 쿠키 이름과 토큰 값이 일치한다")
        void createAccessTokenCookie_nameAndValueMatch() {
            // given
            stubCookieProperties();
            long maxAge = 3600L;

            // when
            ResponseCookie cookie = cookieUtil.createAccessTokenCookie(SAMPLE_TOKEN, maxAge);

            // then
            assertThat(cookie.getName()).isEqualTo(ACCESS_TOKEN_NAME);
            assertThat(cookie.getValue()).isEqualTo(SAMPLE_TOKEN);
        }

        @Test
        @DisplayName("Access Token Cookie 생성 시 maxAge가 전달된 값으로 설정된다")
        void createAccessTokenCookie_maxAgeIsSet() {
            // given
            stubCookieProperties();
            long maxAge = 7200L;

            // when
            ResponseCookie cookie = cookieUtil.createAccessTokenCookie(SAMPLE_TOKEN, maxAge);

            // then
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(maxAge);
        }

        @Test
        @DisplayName("Access Token Cookie 생성 시 SameSite가 Lax로 설정된다")
        void createAccessTokenCookie_sameSiteIsLax() {
            // given
            stubCookieProperties();
            long maxAge = 3600L;

            // when
            ResponseCookie cookie = cookieUtil.createAccessTokenCookie(SAMPLE_TOKEN, maxAge);

            // then
            assertThat(cookie.getSameSite()).isEqualTo(SAME_SITE);
        }
    }

    @Nested
    @DisplayName("createLogoutCookie 테스트")
    class CreateLogoutCookieTest {

        @Test
        @DisplayName("로그아웃 쿠키 생성 시 maxAge가 0으로 설정된다")
        void createLogoutCookie_maxAgeIsZero() {
            // given
            stubCookieProperties();

            // when
            ResponseCookie cookie = cookieUtil.createLogoutCookie();

            // then
            assertThat(cookie.getMaxAge().getSeconds()).isZero();
        }

        @Test
        @DisplayName("로그아웃 쿠키 이름이 Access Token 쿠키 이름과 일치한다")
        void createLogoutCookie_nameMatchesAccessTokenName() {
            // given
            stubCookieProperties();

            // when
            ResponseCookie cookie = cookieUtil.createLogoutCookie();

            // then
            assertThat(cookie.getName()).isEqualTo(ACCESS_TOKEN_NAME);
        }
    }

    @Nested
    @DisplayName("extractAccessTokenFromCookie 테스트")
    class ExtractAccessTokenFromCookieTest {

        @Test
        @DisplayName("요청에 Access Token 쿠키가 있으면 토큰 값을 반환한다")
        void extractAccessTokenFromCookie_success() {
            // given
            given(cookieProperties.getAccessTokenName()).willReturn(ACCESS_TOKEN_NAME);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(ACCESS_TOKEN_NAME, SAMPLE_TOKEN));

            // when
            Optional<String> result = cookieUtil.extractAccessTokenFromCookie(request);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(SAMPLE_TOKEN);
        }

        @Test
        @DisplayName("요청에 쿠키가 없으면 Optional.empty()를 반환한다")
        void extractAccessTokenFromCookie_noCookies_returnsEmpty() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            // 쿠키를 설정하지 않아 request.getCookies() == null

            // when
            Optional<String> result = cookieUtil.extractAccessTokenFromCookie(request);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("요청에 Access Token 쿠키가 없으면 Optional.empty()를 반환한다")
        void extractAccessTokenFromCookie_tokenCookieNotPresent_returnsEmpty() {
            // given
            given(cookieProperties.getAccessTokenName()).willReturn(ACCESS_TOKEN_NAME);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("other_cookie", "other_value"));

            // when
            Optional<String> result = cookieUtil.extractAccessTokenFromCookie(request);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractCookieValue 테스트")
    class ExtractCookieValueTest {

        @Test
        @DisplayName("지정한 이름의 쿠키가 있으면 해당 값을 반환한다")
        void extractCookieValue_success() {
            // given
            String cookieName = "custom_cookie";
            String cookieValue = "custom_value";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie(cookieName, cookieValue));

            // when
            Optional<String> result = cookieUtil.extractCookieValue(request, cookieName);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(cookieValue);
        }

        @Test
        @DisplayName("지정한 이름의 쿠키가 없으면 Optional.empty()를 반환한다")
        void extractCookieValue_notFound_returnsEmpty() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("another_cookie", "value"));

            // when
            Optional<String> result = cookieUtil.extractCookieValue(request, "non_existing");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("쿠키가 전혀 없는 요청이면 Optional.empty()를 반환한다")
        void extractCookieValue_noCookies_returnsEmpty() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when
            Optional<String> result = cookieUtil.extractCookieValue(request, "any_cookie");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAccessTokenCookieName 테스트")
    class GetAccessTokenCookieNameTest {

        @Test
        @DisplayName("설정된 Access Token 쿠키 이름을 반환한다")
        void getAccessTokenCookieName_returnsConfiguredName() {
            // given
            given(cookieProperties.getAccessTokenName()).willReturn(ACCESS_TOKEN_NAME);

            // when
            String name = cookieUtil.getAccessTokenCookieName();

            // then
            assertThat(name).isEqualTo(ACCESS_TOKEN_NAME);
        }
    }
}
