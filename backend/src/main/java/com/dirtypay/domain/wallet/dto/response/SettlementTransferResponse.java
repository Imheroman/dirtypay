package com.dirtypay.domain.wallet.dto.response;

import com.dirtypay.domain.wallet.entity.SettlementTransfer;
import com.dirtypay.domain.wallet.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 정산 송금 응답 DTO.
 *
 * <p>정산 송금 생성, 취소, 조회 시 클라이언트에게 전달되는 데이터를 담는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class SettlementTransferResponse {

    /** 정산 송금 ID. */
    private Long id;

    /** 세션 ID. */
    private Long sessionId;

    /** 조직 멤버 ID (송금자). */
    private Long orgMemberId;

    /** 보내는 지갑 ID. */
    private Long senderWalletId;

    /** 받는 지갑 ID (총무). */
    private Long receiverWalletId;

    /** 송금 금액. */
    private BigDecimal amount;

    /** 송금 상태. */
    private TransferStatus status;

    /** 생성 일시. */
    private LocalDateTime createdDate;

    /**
     * {@link SettlementTransfer} 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param transfer 정산 송금 엔티티
     * @return SettlementTransferResponse 인스턴스
     */
    public static SettlementTransferResponse from(SettlementTransfer transfer) {
        return SettlementTransferResponse.builder()
                .id(transfer.getId())
                .sessionId(transfer.getSessionId())
                .orgMemberId(transfer.getOrgMemberId())
                .senderWalletId(transfer.getSenderWalletId())
                .receiverWalletId(transfer.getReceiverWalletId())
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .createdDate(transfer.getCreatedDate())
                .build();
    }
}
