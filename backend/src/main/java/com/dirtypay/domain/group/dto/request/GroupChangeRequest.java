package com.dirtypay.domain.group.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹 변경 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class GroupChangeRequest {

    @NotNull(message = "이동할 대상 그룹 ID는 필수입니다")
    private Long toGroupId;
}
