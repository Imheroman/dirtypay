package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 조직도 멤버 생성 요청 DTO.
 *
 * <p>userId가 null이면 비회원(미가입) 참여자로 생성된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrgMemberCreateRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;

    private Long userId;
}
