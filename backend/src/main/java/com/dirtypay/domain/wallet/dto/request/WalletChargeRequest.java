package com.dirtypay.domain.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 지갑 충전 요청 DTO.
 *
 * <p>지갑에 금액을 충전할 때 클라이언트로부터 전달받는 요청 데이터를 담는다.
 * 충전 금액은 1원 이상이어야 한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class WalletChargeRequest {

    @NotNull(message = "충전 금액은 필수입니다")
    @DecimalMin(value = "1", message = "충전 금액은 1원 이상이어야 합니다")
    private BigDecimal amount;
}
