package com.dirtypay.domain.settlement.dto.response;

import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 중심 정산 응답 DTO.
 *
 * <p>세션의 전체 주문을 카테고리별로 그룹화하여 정산 결과를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderSettlementResponse {

    private Long sessionId;
    private BigDecimal totalAmount;
    private RemainderStrategyType strategy;
    private List<OrderGroupResponse> orderGroups;
}
