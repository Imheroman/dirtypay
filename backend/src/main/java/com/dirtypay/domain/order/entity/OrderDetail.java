package com.dirtypay.domain.order.entity;

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
 * 주문 상세 엔티티.
 *
 * <p>주문에 참여하는 멤버와 그 분담 비율을 나타낸다.
 * shareRatio는 정산 시 분배 비율로 사용된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "order_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class OrderDetail extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "org_member_id", nullable = false)
    private Long orgMemberId;

    @Column(nullable = false)
    private int shareRatio;

    @Builder
    public OrderDetail(Long orderId, Long orgMemberId, Integer shareRatio) {
        this.orderId = orderId;
        this.orgMemberId = orgMemberId;
        this.shareRatio = shareRatio != null ? shareRatio : 1;
    }
}
