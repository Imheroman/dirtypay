package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 멤버별 주문 상세 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberOrderDetail {

    private Long orderId;
    private String menuName;
    private int quantity;
    private BigDecimal totalPrice;
    private BigDecimal myShare;
}
