package com.dirtypay.domain.organization.dto.response;

import com.dirtypay.domain.organization.entity.Node;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 노드 트리 응답 DTO.
 *
 * <p>조직도 노드를 트리 구조로 표현하며,
 * 각 노드에 소속 멤버 목록을 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeTreeResponse {

    private Long id;
    private Long parentNodeId;
    private String name;
    private int depth;
    private int sortOrder;
    private boolean isSystem;
    private boolean isUnassigned;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @Builder.Default
    private List<OrgMemberResponse> members = new ArrayList<>();

    @Builder.Default
    private List<NodeTreeResponse> children = new ArrayList<>();

    /**
     * Node 엔티티로부터 트리 응답 DTO를 생성한다.
     *
     * <p>members와 children은 빈 리스트로 초기화되며,
     * 이후 트리 빌드 과정에서 채워진다.</p>
     *
     * @param node 노드 엔티티
     * @return 트리 응답 DTO
     */
    public static NodeTreeResponse from(Node node) {
        return NodeTreeResponse.builder()
                .id(node.getId())
                .parentNodeId(node.getParentNodeId())
                .name(node.getName())
                .depth(node.getDepth())
                .sortOrder(node.getSortOrder())
                .isSystem(node.isSystem())
                .isUnassigned(node.isSystem() && !node.isRoot())
                .createdDate(node.getCreatedDate())
                .updatedDate(node.getUpdatedDate())
                .members(new ArrayList<>())
                .children(new ArrayList<>())
                .build();
    }
}
