package com.dirtypay.domain.round.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

/**
 * 라운드 참여자 엔티티.
 *
 * <p>라운드 생성 시 조직도의 OrgMember를 스냅샷하여 생성된다.
 * 각 참여자는 제외(exclude) 처리가 가능하며, 제외된 참여자는 정산에서 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "round_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class RoundParticipant extends BaseEntity {

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "org_member_id", nullable = false)
    private Long orgMemberId;

    @Column(nullable = false)
    private boolean isExcluded;

    @Builder
    public RoundParticipant(Long roundId, Long orgMemberId, Boolean isExcluded) {
        this.roundId = roundId;
        this.orgMemberId = orgMemberId;
        this.isExcluded = isExcluded != null ? isExcluded : false;
    }

    /**
     * 참여자를 정산에서 제외한다.
     */
    public void exclude() {
        this.isExcluded = true;
    }

    /**
     * 참여자를 정산에 포함한다.
     */
    public void include() {
        this.isExcluded = false;
    }
}
