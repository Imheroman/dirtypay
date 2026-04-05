package com.dirtypay.domain.wallet.dto.response;

import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.domain.wallet.entity.WalletStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 지갑 정보 응답 DTO.
 *
 * <p>지갑 생성, 조회, 충전 응답 시 클라이언트에게 전달되는 데이터를 담는다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class WalletResponse {

    private Long id;
    private Long memberId;
    private BigDecimal balance;
    private BigDecimal dailyChargedAmount;
    private WalletStatus status;
    private LocalDateTime createdDate;

    /**
     * Wallet 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param wallet 지갑 엔티티
     * @return WalletResponse 인스턴스
     */
    public static WalletResponse from(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .memberId(wallet.getMemberId())
                .balance(wallet.getBalance())
                .dailyChargedAmount(wallet.getDailyChargedAmount())
                .status(wallet.getStatus())
                .createdDate(wallet.getCreatedDate())
                .build();
    }
}
