package com.dirtypay.domain.settlement.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 정산 완료 상태 엔티티.
 *
 * <p>세션 내 멤버별 정산 납부 금액과 완료 여부를 영속화한다.
 * sessionId + orgMemberId 조합으로 유일 제약을 가진다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "settlement_payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_payment_session_member",
                columnNames = {"session_id", "org_member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class SettlementPayment extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "org_member_id", nullable = false)
    private Long orgMemberId;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal paidAmount;

    @Column(nullable = false)
    private boolean isPaid;

    @Builder
    public SettlementPayment(Long sessionId, Long orgMemberId) {
        this.sessionId = sessionId;
        this.orgMemberId = orgMemberId;
        this.paidAmount = BigDecimal.ZERO;
        this.isPaid = false;
    }

    /**
     * 납부 금액을 업데이트하고 완료 여부를 자동 판정한다.
     *
     * <p>paidAmount가 totalAmount 이상이면 isPaid를 true로 설정한다.</p>
     *
     * @param paidAmount  납부 금액
     * @param totalAmount 정산 총액
     */
    public void updatePayment(BigDecimal paidAmount, BigDecimal totalAmount) {
        this.paidAmount = paidAmount;
        this.isPaid = paidAmount.compareTo(totalAmount) >= 0;
    }
}
