package com.dirtypay.domain.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie 생성/삭제/조회 유틸리티.
 *
 * <p>Access Token을 HttpOnly Cookie로 관리하기 위한 유틸리티 메서드를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final CookieProperties cookieProperties;

    /**
     * Access Token Cookie를 생성한다.
     *
     * <p>보안 설정:</p>
     * <ul>
     *   <li>HttpOnly: JavaScript 접근 차단 (XSS 방지)</li>
     *   <li>Secure: HTTPS에서만 전송 (운영 환경)</li>
     *   <li>SameSite: CSRF 공격 완화</li>
     *   <li>Path: /api 하위에서만 전송</li>
     * </ul>
     *
     * @param token  Access Token 값
     * @param maxAge 만료 시간 (초)
     * @return ResponseCookie 객체
     */
    public ResponseCookie createAccessTokenCookie(String token, long maxAge) {
        return ResponseCookie.from(this.cookieProperties.getAccessTokenName(), token)
                .httpOnly(this.cookieProperties.isHttpOnly())
                .secure(this.cookieProperties.isSecure())
                .sameSite(this.cookieProperties.getSameSite())
                .path(this.cookieProperties.getPath())
                .domain(this.cookieProperties.getDomain())
                .maxAge(maxAge)
                .build();
    }

    /**
     * Access Token Cookie를 삭제하기 위한 Cookie를 생성한다.
     *
     * <p>MaxAge를 0으로 설정하여 브라우저가 Cookie를 삭제하도록 한다.</p>
     *
     * @return 삭제용 ResponseCookie 객체
     */
    public ResponseCookie createLogoutCookie() {
        return ResponseCookie.from(this.cookieProperties.getAccessTokenName(), "")
                .httpOnly(this.cookieProperties.isHttpOnly())
                .secure(this.cookieProperties.isSecure())
                .sameSite(this.cookieProperties.getSameSite())
                .path(this.cookieProperties.getPath())
                .domain(this.cookieProperties.getDomain())
                .maxAge(0)
                .build();
    }

    /**
     * HTTP 요청에서 Access Token Cookie를 추출한다.
     *
     * @param request HTTP 요청
     * @return Access Token 값 Optional
     */
    public Optional<String> extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> this.cookieProperties.getAccessTokenName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * 특정 이름의 Cookie 값을 추출한다.
     *
     * @param request    HTTP 요청
     * @param cookieName Cookie 이름
     * @return Cookie 값 Optional
     */
    public Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * Access Token Cookie 이름을 반환한다.
     *
     * @return Cookie 이름
     */
    public String getAccessTokenCookieName() {
        return this.cookieProperties.getAccessTokenName();
    }
}
