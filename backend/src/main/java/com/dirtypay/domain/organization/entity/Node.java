package com.dirtypay.domain.organization.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

/**
 * 조직도 노드 엔티티.
 *
 * <p>Session 내 조직 구조를 트리 형태로 표현한다.
 * Self-Reference(parentNodeId)로 부모-자식 관계를 구성하며,
 * 최대 5depth(0~4)를 지원한다.</p>
 *
 * <p>루트 노드의 parentNodeId는 null이다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "nodes", indexes = {
        @Index(name = "idx_node_session_parent", columnList = "session_id, parent_node_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Node extends BaseEntity {

    private static final int MAX_DEPTH = 4;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "parent_node_id")
    private Long parentNodeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean isSystem;

    @Builder
    public Node(Long sessionId, Long parentNodeId, String name, int depth, int sortOrder, boolean isSystem) {
        validateDepth(depth);
        this.sessionId = sessionId;
        this.parentNodeId = parentNodeId;
        this.name = name;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.isSystem = isSystem;
    }

    /**
     * 노드 정보를 수정한다.
     *
     * @param name      노드 이름
     * @param sortOrder 정렬 순서
     */
    public void update(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    /**
     * 부모 노드를 변경한다. (Reparenting)
     *
     * @param parentNodeId 새 부모 노드 ID (null이면 루트)
     * @param depth        새 depth
     */
    public void move(Long parentNodeId, int depth) {
        validateDepth(depth);
        this.parentNodeId = parentNodeId;
        this.depth = depth;
    }

    /**
     * 루트 노드인지 확인한다.
     *
     * @return 루트 노드 여부
     */
    public boolean isRoot() {
        return this.parentNodeId == null;
    }

    private void validateDepth(int depth) {
        if (depth < 0 || depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    String.format("Node depth는 0~%d 범위여야 합니다. 입력값: %d", MAX_DEPTH, depth));
        }
    }
}
