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
import java.time.LocalDate;

/**
 * 가상 지갑 엔티티.
 *
 * <p>회원 1인당 하나의 지갑을 소유하며, 충전·출금·송금 기능을 제공한다.
 * 일일 충전 한도 관리를 위해 {@code dailyChargedAmount}와 {@code lastChargedDate}를 함께 관리한다.
 * Soft Delete를 지원하여 deletedDate가 null이 아닌 경우 삭제된 것으로 간주한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "wallets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Wallet extends BaseEntity {

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal balance;

    @Column(name = "daily_charged_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal dailyChargedAmount;

    @Column(name = "last_charged_date")
    private LocalDate lastChargedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    /**
     * Wallet 엔티티를 생성한다.
     *
     * <p>잔액과 일일 충전 누적액은 {@link BigDecimal#ZERO}로 초기화되며,
     * 상태는 {@link WalletStatus#ACTIVE}로 설정된다.</p>
     *
     * @param memberId 지갑 소유 회원 ID
     */
    @Builder
    public Wallet(Long memberId) {
        this.memberId = memberId;
        this.balance = BigDecimal.ZERO;
        this.dailyChargedAmount = BigDecimal.ZERO;
        this.lastChargedDate = null;
        this.status = WalletStatus.ACTIVE;
    }

    /**
     * 지갑에 금액을 충전한다.
     *
     * <p>잔액과 일일 충전 누적액을 동시에 증가시킨다.
     * 지갑이 {@link WalletStatus#ACTIVE} 상태가 아니거나 금액이 유효하지 않으면 예외를 던진다.</p>
     *
     * @param amount 충전 금액 (양수여야 한다)
     * @throws BusinessException {@code WALLET_NOT_ACTIVE} — 지갑이 활성 상태가 아닌 경우
     * @throws BusinessException {@code WALLET_INVALID_AMOUNT} — 금액이 null이거나 0 이하인 경우
     */
    public void charge(BigDecimal amount) {
        validateActive();
        validateAmount(amount);
        this.balance = this.balance.add(amount);
        this.dailyChargedAmount = this.dailyChargedAmount.add(amount);
        this.lastChargedDate = LocalDate.now();
    }

    /**
     * 지갑에서 금액을 출금한다.
     *
     * <p>지갑이 {@link WalletStatus#ACTIVE} 상태가 아니거나, 금액이 유효하지 않거나,
     * 잔액이 부족하면 예외를 던진다.</p>
     *
     * @param amount 출금 금액 (양수여야 한다)
     * @throws BusinessException {@code WALLET_NOT_ACTIVE} — 지갑이 활성 상태가 아닌 경우
     * @throws BusinessException {@code WALLET_INVALID_AMOUNT} — 금액이 null이거나 0 이하인 경우
     * @throws BusinessException {@code WALLET_INSUFFICIENT_BALANCE} — 잔액이 부족한 경우
     */
    public void withdraw(BigDecimal amount) {
        validateActive();
        validateAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * 지갑에 금액을 입금한다.
     *
     * <p>송금 수신 등 외부로부터 금액이 유입될 때 사용한다.
     * 지갑이 {@link WalletStatus#ACTIVE} 상태가 아니거나 금액이 유효하지 않으면 예외를 던진다.</p>
     *
     * @param amount 입금 금액 (양수여야 한다)
     * @throws BusinessException {@code WALLET_NOT_ACTIVE} — 지갑이 활성 상태가 아닌 경우
     * @throws BusinessException {@code WALLET_INVALID_AMOUNT} — 금액이 null이거나 0 이하인 경우
     */
    public void deposit(BigDecimal amount) {
        validateActive();
        validateAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * 필요 시 일일 충전 한도를 초기화한다.
     *
     * <p>마지막 충전일({@code lastChargedDate})이 오늘 날짜와 다른 경우
     * {@code dailyChargedAmount}를 {@link BigDecimal#ZERO}로 리셋한다.
     * 충전 전 호출하여 당일 누적액을 정확하게 유지한다.</p>
     */
    public void resetDailyLimitIfNeeded() {
        if (this.lastChargedDate == null || !this.lastChargedDate.isEqual(LocalDate.now())) {
            this.dailyChargedAmount = BigDecimal.ZERO;
        }
    }

    /**
     * 지갑이 활성 상태인지 검증한다.
     *
     * @throws BusinessException {@code WALLET_NOT_ACTIVE} — 지갑이 ACTIVE 상태가 아닌 경우
     */
    private void validateActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.WALLET_NOT_ACTIVE);
        }
    }

    /**
     * 금액 값이 유효한지 검증한다.
     *
     * @param amount 검증할 금액
     * @throws BusinessException {@code WALLET_INVALID_AMOUNT} — 금액이 null이거나 0 이하인 경우
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.WALLET_INVALID_AMOUNT);
        }
    }
}
