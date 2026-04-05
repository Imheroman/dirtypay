package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 멤버별 정산 상세 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberSettlementResponse {

    private Long orgMemberId;
    private BigDecimal totalAmount;
    private boolean isPaid;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private List<MemberRoundDetail> details;
}
