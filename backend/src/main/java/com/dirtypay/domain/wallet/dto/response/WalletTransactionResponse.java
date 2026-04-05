package com.dirtypay.domain.wallet.dto.response;

import com.dirtypay.domain.wallet.entity.TransactionStatus;
import com.dirtypay.domain.wallet.entity.TransactionType;
import com.dirtypay.domain.wallet.entity.WalletTransaction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 지갑 거래 이력 응답 DTO.
 *
 * <p>지갑 거래 이력 조회 응답 시 클라이언트에게 전달되는 데이터를 담는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class WalletTransactionResponse {

    private Long id;
    private Long walletId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Long counterpartyWalletId;
    private String referenceType;
    private Long referenceId;
    private String description;
    private TransactionStatus status;
    private LocalDateTime createdDate;

    /**
     * WalletTransaction 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param tx 거래 이력 엔티티
     * @return WalletTransactionResponse 인스턴스
     */
    public static WalletTransactionResponse from(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWalletId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceBefore(tx.getBalanceBefore())
                .balanceAfter(tx.getBalanceAfter())
                .counterpartyWalletId(tx.getCounterpartyWalletId())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .description(tx.getDescription())
                .status(tx.getStatus())
                .createdDate(tx.getCreatedDate())
                .build();
    }
}
