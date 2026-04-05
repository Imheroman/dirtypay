package com.dirtypay.domain.settlement.dto.response;

import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 라운드 정산 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class RoundSettlementResponse {

    private Long roundId;
    private BigDecimal totalAmount;
    private RemainderStrategyType strategy;
    private List<MemberAmountResponse> settlements;
}
