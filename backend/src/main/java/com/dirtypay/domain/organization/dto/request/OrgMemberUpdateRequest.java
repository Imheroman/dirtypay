package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 조직도 멤버 수정 요청 DTO.
 *
 * <p>isActive가 null이면 활성 상태를 변경하지 않는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrgMemberUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;

    private Boolean isActive;
}
