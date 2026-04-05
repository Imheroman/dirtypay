package com.dirtypay.domain.joinrequest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여 요청 생성 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class JoinRequestCreateRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 20, message = "닉네임은 최대 20자입니다")
    private String nickname;

    @Size(max = 500, message = "메시지는 최대 500자입니다")
    private String message;
}
