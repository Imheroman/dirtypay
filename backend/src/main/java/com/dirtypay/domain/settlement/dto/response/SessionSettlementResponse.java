package com.dirtypay.domain.settlement.dto.response;

import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 세션 정산 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class SessionSettlementResponse {

    private Long sessionId;
    private BigDecimal totalAmount;
    private RemainderStrategyType strategy;
    private List<MemberAmountResponse> settlements;
    private List<RoundSettlementResponse> rounds;
}
