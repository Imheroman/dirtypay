package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문별 정산 메뉴 항목 응답 DTO.
 *
 * <p>개별 메뉴에 대한 주문 정보와 참여 멤버 지분을 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderSettlementItemResponse {

    private Long roundId;
    private Long menuId;
    private String menuName;
    private BigDecimal menuPrice;
    private int quantity;
    private BigDecimal totalPrice;
    private List<OrderMemberShareResponse> members;
}
