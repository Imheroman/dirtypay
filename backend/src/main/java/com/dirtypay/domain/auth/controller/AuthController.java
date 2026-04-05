package com.dirtypay.domain.auth.controller;

import com.dirtypay.domain.auth.dto.request.LoginRequest;
import com.dirtypay.domain.auth.dto.request.RefreshTokenRequest;
import com.dirtypay.domain.auth.dto.request.SignupRequest;
import com.dirtypay.domain.auth.dto.response.LoginResponse;
import com.dirtypay.domain.auth.dto.response.TokenValidationResponse;
import com.dirtypay.domain.auth.security.CookieUtil;
import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.auth.service.AuthService;
import com.dirtypay.domain.auth.service.AuthService.LoginResult;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러.
 *
 * <p>회원가입, 로그인, 토큰 갱신, 로그아웃 등의 인증 관련 API를 제공한다.</p>
 *
 * <p>Access Token은 HttpOnly Cookie로 전달되며,
 * Refresh Token은 Response Body로 전달된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    /**
     * 새로운 회원을 등록한다.
     *
     * @param request 회원가입 요청 DTO
     * @return 등록된 회원 정보
     */
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signup(
            @Valid @RequestBody SignupRequest request) {

        MemberResponse response = this.authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 이메일과 비밀번호로 로그인한다.
     *
     * <p>로그인 성공 시:</p>
     * <ul>
     *   <li>Access Token: HttpOnly Cookie로 설정</li>
     *   <li>Refresh Token: Response Body로 전달</li>
     * </ul>
     *
     * @param request 로그인 요청 DTO
     * @return 로그인 응답 (Refresh Token, 만료 시간)
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. Access Token은 Cookie로 설정됩니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result = this.authService.login(request);

        // Access Token을 HttpOnly Cookie로 설정
        ResponseCookie accessTokenCookie = this.cookieUtil.createAccessTokenCookie(
                result.accessToken(),
                result.accessTokenExpiresIn()
        );

        // Response Body에는 Refresh Token + 사용자 정보 포함
        LoginResponse response = LoginResponse.of(
                result.refreshToken(),
                result.accessTokenExpiresIn(),
                result.refreshTokenExpiresIn(),
                MemberResponse.from(result.member())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(ApiResponse.success(response));
    }

    /**
     * Refresh Token으로 새 Access Token을 발급받는다.
     *
     * <p>새 Access Token은 Cookie로 설정되고,
     * 새 Refresh Token은 Response Body로 전달된다.</p>
     *
     * @param request Refresh Token 요청 DTO
     * @return 새 토큰 정보
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        LoginResult result = this.authService.refreshAccessToken(request);

        // 새 Access Token을 HttpOnly Cookie로 설정
        ResponseCookie accessTokenCookie = this.cookieUtil.createAccessTokenCookie(
                result.accessToken(),
                result.accessTokenExpiresIn()
        );

        // Response Body에는 새 Refresh Token + 사용자 정보 포함
        LoginResponse response = LoginResponse.of(
                result.refreshToken(),
                result.accessTokenExpiresIn(),
                result.refreshTokenExpiresIn(),
                MemberResponse.from(result.member())
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(ApiResponse.success(response));
    }

    /**
     * Refresh Token의 유효성을 검증한다.
     *
     * @param request Refresh Token 요청 DTO (Request Body)
     * @return 유효성 검증 결과
     */
    @Operation(summary = "토큰 검증", description = "Refresh Token의 유효성과 만료 시간을 확인합니다.")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenValidationResponse response = this.authService.validateRefreshToken(request.getRefreshToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 로그아웃한다.
     *
     * <p>다음 작업을 수행한다:</p>
     * <ul>
     *   <li>DB에서 Refresh Token 삭제</li>
     *   <li>Access Token을 Redis 블랙리스트에 등록</li>
     *   <li>Access Token Cookie 삭제</li>
     * </ul>
     *
     * @param request       HTTP 요청 (Access Token 추출 용도)
     * @param userPrincipal 인증된 사용자 정보
     * @return 빈 응답
     */
    @Operation(summary = "로그아웃", description = "로그아웃하고 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String accessToken = this.resolveAccessToken(request);
        this.authService.logout(userPrincipal.getId(), accessToken);

        // Access Token Cookie 삭제
        ResponseCookie logoutCookie = this.cookieUtil.createLogoutCookie();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString())
                .body(ApiResponse.success(null));
    }

    /**
     * HTTP 요청에서 Access Token을 추출한다.
     *
     * <p>추출 우선순위:</p>
     * <ol>
     *   <li>Cookie의 access_token</li>
     *   <li>Authorization 헤더의 Bearer 토큰</li>
     * </ol>
     *
     * @param request HTTP 요청
     * @return Access Token 문자열, 없으면 null
     */
    private String resolveAccessToken(HttpServletRequest request) {
        return this.cookieUtil.extractAccessTokenFromCookie(request)
                .orElseGet(() -> {
                    String header = request.getHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        return header.substring(7);
                    }
                    return null;
                });
    }
}
