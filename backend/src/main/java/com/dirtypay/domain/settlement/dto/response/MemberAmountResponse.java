package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 멤버별 정산 금액 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberAmountResponse {

    private Long orgMemberId;
    private String nickname;
    private BigDecimal amount;
    private boolean isExcluded;
    private boolean isPaid;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
}
