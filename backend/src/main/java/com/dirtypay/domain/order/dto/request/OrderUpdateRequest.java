package com.dirtypay.domain.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 수정 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrderUpdateRequest {

    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    @Max(value = 50, message = "수량은 최대 50개입니다")
    private int quantity;
}
