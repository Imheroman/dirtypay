package com.dirtypay.domain.organization.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 노드 이동 요청 DTO.
 *
 * <p>노드의 부모를 변경하여 트리 내 위치를 이동한다.
 * targetParentNodeId가 null이면 루트 노드로 이동한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class NodeMoveRequest {

    /**
     * 이동 대상 부모 노드 ID. null이면 루트 노드로 이동.
     */
    private Long targetParentNodeId;

    /**
     * 이동 후 정렬 순서.
     */
    @NotNull
    private Integer sortOrder;
}
