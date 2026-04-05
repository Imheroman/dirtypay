package com.dirtypay.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 유효성 검증 응답 DTO.
 *
 * <p>Refresh Token의 만료 여부와 남은 시간을 반환한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class TokenValidationResponse {

    /**
     * 토큰 유효 여부.
     */
    private boolean valid;

    /**
     * 토큰 만료까지 남은 시간 (초).
     * 만료되었거나 유효하지 않으면 0.
     */
    private long expiresInSeconds;

    /**
     * 응답 메시지.
     */
    private String message;

    /**
     * 유효한 토큰 응답을 생성한다.
     *
     * @param expiresInSeconds 만료까지 남은 시간 (초)
     * @return TokenValidationResponse 인스턴스
     */
    public static TokenValidationResponse valid(long expiresInSeconds) {
        return TokenValidationResponse.builder()
                .valid(true)
                .expiresInSeconds(expiresInSeconds)
                .message("Token is valid")
                .build();
    }

    /**
     * 만료된 토큰 응답을 생성한다.
     *
     * @return TokenValidationResponse 인스턴스
     */
    public static TokenValidationResponse expired() {
        return TokenValidationResponse.builder()
                .valid(false)
                .expiresInSeconds(0)
                .message("Token has expired")
                .build();
    }

    /**
     * 유효하지 않은 토큰 응답을 생성한다.
     *
     * @param message 오류 메시지
     * @return TokenValidationResponse 인스턴스
     */
    public static TokenValidationResponse invalid(String message) {
        return TokenValidationResponse.builder()
                .valid(false)
                .expiresInSeconds(0)
                .message(message)
                .build();
    }
}
