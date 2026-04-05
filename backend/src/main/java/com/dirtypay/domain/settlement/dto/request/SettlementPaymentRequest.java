package com.dirtypay.domain.settlement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 정산 완료 표시 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class SettlementPaymentRequest {

    @NotNull(message = "납부 금액은 필수입니다")
    @DecimalMin(value = "0", message = "납부 금액은 0 이상이어야 합니다")
    private BigDecimal paidAmount;
}
