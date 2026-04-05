package com.dirtypay.domain.group.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 라운드 그룹 엔티티.
 *
 * <p>라운드 내에서 메뉴 공유를 위한 그룹을 나타낸다.
 * 자기 참조(Self-referencing)를 통해 계층 구조(트리)를 형성한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "round_groups", indexes = {
        @Index(name = "idx_round_groups_round_parent", columnList = "round_id, parent_group_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class RoundGroup extends BaseEntity {

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "parent_group_id")
    private Long parentGroupId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int depth;

    @Builder
    public RoundGroup(Long roundId, Long parentGroupId, String name, int depth) {
        this.roundId = roundId;
        this.parentGroupId = parentGroupId;
        this.name = name;
        this.depth = depth;
    }

    /**
     * 그룹명을 수정한다.
     *
     * @param name 수정할 그룹명
     */
    public void updateName(String name) {
        this.name = name;
    }
}
