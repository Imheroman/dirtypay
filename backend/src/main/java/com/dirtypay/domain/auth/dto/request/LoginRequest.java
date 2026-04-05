package com.dirtypay.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다")
    private String password;
}
