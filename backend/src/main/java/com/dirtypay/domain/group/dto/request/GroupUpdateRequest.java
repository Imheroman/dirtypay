package com.dirtypay.domain.group.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹 수정 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class GroupUpdateRequest {

    @NotBlank(message = "그룹명은 필수입니다")
    private String name;
}
