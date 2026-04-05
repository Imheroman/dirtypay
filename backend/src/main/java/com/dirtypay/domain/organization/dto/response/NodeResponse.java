package com.dirtypay.domain.organization.dto.response;

import com.dirtypay.domain.organization.entity.Node;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 노드 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeResponse {

    private Long id;
    private Long sessionId;
    private Long parentNodeId;
    private String name;
    private int depth;
    private int sortOrder;
    private boolean isSystem;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Node 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param node 노드 엔티티
     * @return 노드 응답 DTO
     */
    public static NodeResponse from(Node node) {
        return NodeResponse.builder()
                .id(node.getId())
                .sessionId(node.getSessionId())
                .parentNodeId(node.getParentNodeId())
                .name(node.getName())
                .depth(node.getDepth())
                .sortOrder(node.getSortOrder())
                .isSystem(node.isSystem())
                .createdDate(node.getCreatedDate())
                .updatedDate(node.getUpdatedDate())
                .build();
    }
}
