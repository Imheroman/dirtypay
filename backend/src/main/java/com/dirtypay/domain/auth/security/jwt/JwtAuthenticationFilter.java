package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.domain.auth.security.CookieUtil;
import com.dirtypay.domain.auth.security.blacklist.BlacklistCheckService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터.
 *
 * <p>모든 HTTP 요청에서 JWT 토큰을 검증하고,
 * 유효한 토큰인 경우 SecurityContext에 인증 정보를 설정한다.</p>
 *
 * <p>토큰 추출 우선순위:</p>
 * <ol>
 *   <li>Authorization 헤더 (Bearer 토큰)</li>
 *   <li>HttpOnly Cookie (access_token)</li>
 * </ol>
 *
 * <p>블랙리스트 조회 흐름 (Spring Profile에 따라 구현체가 결정됨):</p>
 * <ol>
 *   <li>JWT 서명/만료 검증 통과 후 jti를 추출한다</li>
 *   <li>{@code BlacklistCheckService}를 통해 jti의 블랙리스트 등록 여부를 확인한다</li>
 *   <li>블랙리스트에 등록된 경우 즉시 401 Unauthorized를 반환한다</li>
 *   <li>블랙리스트에 없는 경우 정상 인증 흐름을 계속 진행한다</li>
 * </ol>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final BlacklistCheckService blacklistCheckService;

    /**
     * JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정한다.
     *
     * <p>블랙리스트 조회는 JWT 서명 검증 이후에 수행된다.
     * Spring Profile에 따라 NoOp(기존 동작 유지), DB 조회, Redis 조회 중 하나가 활성화된다.</p>
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException      IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = this.resolveToken(request);

        if (StringUtils.hasText(token) && this.jwtTokenProvider.validateAccessToken(token)) {
            // 블랙리스트 조회 (NoOp: 항상 통과, DB/Redis: 실제 조회)
            String jti = this.jwtTokenProvider.extractJti(token);
            if (jti != null && this.blacklistCheckService.isBlacklisted(jti)) {
                log.debug("Blacklisted token rejected: jti={}, uri={}", jti, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            Authentication authentication = this.jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Set Authentication to security context for '{}', uri: {}",
                    authentication.getName(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출한다.
     *
     * <p>추출 우선순위:</p>
     * <ol>
     *   <li>Authorization 헤더의 Bearer 토큰</li>
     *   <li>Cookie의 access_token</li>
     * </ol>
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열, 없으면 null
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 추출 시도
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 2. Cookie에서 Access Token 추출 시도
        return this.cookieUtil.extractAccessTokenFromCookie(request)
                .orElse(null);
    }
}
