package com.dirtypay.domain.wallet.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 거래 이력 엔티티.
 *
 * <p>지갑에서 발생하는 모든 거래(충전·송금·환불)의 이력을 영속화한다.
 * {@code idempotencyKey}로 중복 거래를 방지하며, Soft Delete를 지원하여
 * deletedDate가 null이 아닌 경우 삭제된 것으로 간주한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "wallet_transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wallet_transaction_idempotency",
                columnNames = {"idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class WalletTransaction extends BaseEntity {

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 0)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 0)
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_wallet_id")
    private Long counterpartyWalletId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    /**
     * WalletTransaction 엔티티를 생성한다.
     *
     * @param walletId              소속 지갑 ID
     * @param type                  거래 유형 ({@link TransactionType})
     * @param amount                거래 금액
     * @param balanceBefore         거래 전 잔액
     * @param balanceAfter          거래 후 잔액
     * @param counterpartyWalletId  상대방 지갑 ID (송금 시, nullable)
     * @param referenceType         연관 엔티티 타입 (nullable, 예: "SETTLEMENT")
     * @param referenceId           연관 엔티티 ID (nullable)
     * @param idempotencyKey        멱등성 보장 키
     * @param description           거래 설명 (nullable)
     * @param status                거래 상태 ({@link TransactionStatus})
     */
    @Builder
    public WalletTransaction(
            Long walletId,
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            Long counterpartyWalletId,
            String referenceType,
            Long referenceId,
            String idempotencyKey,
            String description,
            TransactionStatus status) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.counterpartyWalletId = counterpartyWalletId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.description = description;
        this.status = status;
    }
}
