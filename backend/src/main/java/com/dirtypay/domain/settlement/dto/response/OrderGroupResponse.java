package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문별 정산 카테고리 그룹 응답 DTO.
 *
 * <p>같은 카테고리에 속하는 메뉴 항목들을 그룹화한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderGroupResponse {

    private String category;
    private BigDecimal totalAmount;
    private List<OrderSettlementItemResponse> items;
}
