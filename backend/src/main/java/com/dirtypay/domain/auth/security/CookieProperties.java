package com.dirtypay.domain.auth.security;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cookie 설정 프로퍼티.
 *
 * <p>application.yml의 cookie 설정을 바인딩한다.</p>
 *
 * <p>보안 관련 설정(httpOnly, secure 등)의 런타임 변경을 방지하기 위해
 * 생성자 바인딩 방식으로 불변 객체로 관리한다.</p>
 *
 * <pre>
 * cookie:
 *   domain: localhost
 *   secure: false          # HTTPS에서만 전송 (운영: true)
 *   http-only: true        # JavaScript 접근 차단
 *   same-site: Lax         # CSRF 방지
 *   path: /api             # Cookie 경로
 *   access-token-name: access_token
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    /**
     * Cookie 도메인.
     * 로컬 개발 시 localhost, 운영 시 실제 도메인 설정.
     */
    private final String domain;

    /**
     * HTTPS에서만 Cookie 전송 여부.
     * 운영 환경에서는 반드시 true로 설정해야 한다.
     */
    private final boolean secure;

    /**
     * JavaScript에서 Cookie 접근 차단 여부.
     * XSS 공격 방지를 위해 항상 true로 설정한다.
     */
    private final boolean httpOnly;

    /**
     * SameSite 설정.
     * - Strict: 같은 사이트에서만 Cookie 전송
     * - Lax: GET 요청 시 크로스 사이트 허용 (기본값)
     * - None: 모든 크로스 사이트 요청 허용 (Secure 필수)
     */
    private final String sameSite;

    /**
     * Cookie 경로.
     * 해당 경로 하위에서만 Cookie가 전송된다.
     */
    private final String path;

    /**
     * Access Token Cookie 이름.
     */
    private final String accessTokenName;

    /**
     * 생성자 바인딩을 위한 전체 인자 생성자.
     *
     * <p>Spring Boot 3.x에서 생성자가 하나이면 @ConstructorBinding 없이
     * 자동으로 생성자 바인딩이 적용된다.</p>
     *
     * @param domain          Cookie 도메인
     * @param secure          HTTPS 전용 여부
     * @param httpOnly        JavaScript 접근 차단 여부
     * @param sameSite        SameSite 정책
     * @param path            Cookie 경로
     * @param accessTokenName Access Token Cookie 이름
     */
    public CookieProperties(
            @DefaultValue("localhost") String domain,
            @DefaultValue("false") boolean secure,
            @DefaultValue("true") boolean httpOnly,
            @DefaultValue("Lax") String sameSite,
            @DefaultValue("/api") String path,
            @DefaultValue("access_token") String accessTokenName) {
        this.domain = domain;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
        this.path = path;
        this.accessTokenName = accessTokenName;
    }
}
