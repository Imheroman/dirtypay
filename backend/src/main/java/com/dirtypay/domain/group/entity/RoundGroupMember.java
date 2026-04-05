package com.dirtypay.domain.group.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 라운드 그룹 멤버 엔티티.
 *
 * <p>그룹에 참여하는 조직 멤버를 나타낸다.
 * 한 멤버는 하나의 라운드에서 하나의 그룹에만 참여할 수 있다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "round_group_members",
        indexes = {
                @Index(name = "idx_round_group_members_group", columnList = "group_id"),
                @Index(name = "idx_round_group_members_org_member", columnList = "org_member_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_group_member", columnNames = {"group_id", "org_member_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class RoundGroupMember extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "org_member_id", nullable = false)
    private Long orgMemberId;

    @Builder
    public RoundGroupMember(Long groupId, Long orgMemberId) {
        this.groupId = groupId;
        this.orgMemberId = orgMemberId;
    }
}
