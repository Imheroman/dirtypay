package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노드 생성 요청 DTO.
 *
 * <p>parentNodeId가 null이면 루트 노드로 생성된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class NodeCreateRequest {

    private Long parentNodeId;

    @NotBlank(message = "노드 이름은 필수입니다")
    private String name;

    private int sortOrder;
}
