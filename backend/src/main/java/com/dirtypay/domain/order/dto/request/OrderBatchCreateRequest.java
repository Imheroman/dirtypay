package com.dirtypay.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 일괄 생성 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrderBatchCreateRequest {

    @NotEmpty(message = "주문 목록은 최소 1건 이상이어야 합니다")
    @Valid
    private List<OrderCreateRequest> orders;
}
