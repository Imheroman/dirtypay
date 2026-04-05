package com.dirtypay.domain.auth.dto.response;

import com.dirtypay.domain.member.dto.response.MemberResponse;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 DTO (Cookie 기반).
 *
 * <p>Access Token은 HttpOnly Cookie로 전달되며,
 * Response Body에는 Refresh Token, 메타 정보, 사용자 정보가 포함된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class LoginResponse {

    /**
     * Refresh Token.
     * 프론트엔드 서버에서 안전하게 저장하여 관리한다.
     */
    private String refreshToken;

    /**
     * 토큰 타입 (Bearer).
     */
    private String tokenType;

    /**
     * Access Token 만료 시간 (초).
     */
    private long accessTokenExpiresIn;

    /**
     * Refresh Token 만료 시간 (초).
     */
    private long refreshTokenExpiresIn;

    /**
     * 로그인한 사용자 정보.
     */
    private MemberResponse user;

    /**
     * 로그인 응답을 생성한다.
     *
     * @param refreshToken           Refresh Token
     * @param accessTokenExpiresIn   Access Token 만료 시간 (초)
     * @param refreshTokenExpiresIn  Refresh Token 만료 시간 (초)
     * @param user                   사용자 정보
     * @return LoginResponse 인스턴스
     */
    public static LoginResponse of(
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn,
            MemberResponse user
    ) {
        return LoginResponse.builder()
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .user(user)
                .build();
    }
}
