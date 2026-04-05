package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노드 수정 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class NodeUpdateRequest {

    @NotBlank(message = "노드 이름은 필수입니다")
    private String name;

    private int sortOrder;
}
