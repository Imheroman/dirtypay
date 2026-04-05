package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멤버 노드 이동 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrgMemberMoveRequest {

    @NotNull(message = "이동 대상 노드 ID는 필수입니다")
    private Long targetNodeId;
}
