package com.dirtypay.domain.wallet.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 정산 송금 엔티티.
 *
 * <p>세션 내 조직 멤버별 정산 송금 내역을 영속화한다.
 * 송금자({@code orgMemberId})의 지갑에서 총무({@code receiverWalletId})의 지갑으로 금액을 이체하며,
 * 활성(soft-delete 미적용) 레코드에 한해 sessionId + orgMemberId 조합으로 유일 제약을 가진다.
 *
 * <p>Soft Delete 호환 UNIQUE 제약: JPA {@code @UniqueConstraint}는 삭제된 레코드를 포함하므로
 * 소프트 딜리트된 이력이 존재할 때 신규 송금이 DB 제약 위반을 유발하는 문제가 있다.
 * 이를 방지하기 위해 {@code @Table} 수준의 UNIQUE 제약 대신 Flyway 마이그레이션을 통해
 * {@code active_transfer_key} 가상 컬럼 기반의 부분 UNIQUE 인덱스를 적용한다
 * (마이그레이션 파일: V20260318_04__add_settlement_transfer_partial_unique_index.sql).
 * 삭제된 레코드의 {@code active_transfer_key}는 NULL이므로 MariaDB UNIQUE 인덱스 중복 허용 규칙에 의해
 * 동일 (sessionId, orgMemberId) 조합의 삭제 이력이 있어도 신규 삽입이 가능하다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "settlement_transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class SettlementTransfer extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "org_member_id", nullable = false)
    private Long orgMemberId;

    @Column(name = "sender_wallet_id", nullable = false)
    private Long senderWalletId;

    @Column(name = "receiver_wallet_id", nullable = false)
    private Long receiverWalletId;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    /**
     * SettlementTransfer 엔티티를 생성한다.
     *
     * <p>송금 상태는 {@link TransferStatus#PENDING}으로 초기화된다.</p>
     *
     * @param sessionId        세션 ID
     * @param orgMemberId      조직 멤버 ID (송금자)
     * @param senderWalletId   보내는 지갑 ID
     * @param receiverWalletId 받는 지갑 ID (총무)
     * @param amount           송금 금액
     */
    @Builder
    public SettlementTransfer(Long sessionId, Long orgMemberId, Long senderWalletId,
                               Long receiverWalletId, BigDecimal amount) {
        this.sessionId = sessionId;
        this.orgMemberId = orgMemberId;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.status = TransferStatus.PENDING;
    }

    /**
     * 송금을 완료 처리한다.
     *
     * <p>송금 상태를 {@link TransferStatus#COMPLETED}로 변경한다.</p>
     */
    public void complete() {
        this.status = TransferStatus.COMPLETED;
    }

    /**
     * 송금을 취소 처리한다.
     *
     * <p>송금 상태를 {@link TransferStatus#CANCELLED}로 변경한다.
     * {@link TransferStatus#PENDING} 상태에서만 취소할 수 있다.</p>
     *
     * @throws BusinessException {@code TRANSFER_NOT_CANCELLABLE} — 현재 상태가 PENDING이 아닌 경우
     */
    public void cancel() {
        if (this.status != TransferStatus.PENDING) {
            throw new BusinessException(ErrorCode.TRANSFER_NOT_CANCELLABLE);
        }
        this.status = TransferStatus.CANCELLED;
    }

    /**
     * 송금을 실패 처리한다.
     *
     * <p>송금 상태를 {@link TransferStatus#FAILED}로 변경한다.</p>
     */
    public void fail() {
        this.status = TransferStatus.FAILED;
    }
}
