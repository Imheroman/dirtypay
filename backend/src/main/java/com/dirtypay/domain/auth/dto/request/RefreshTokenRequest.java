package com.dirtypay.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh Token 요청 DTO.
 *
 * <p>Access Token 갱신 시 Refresh Token을 전달받는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

    /**
     * Refresh Token 값.
     */
    @NotBlank(message = "Refresh Token은 필수입니다")
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
