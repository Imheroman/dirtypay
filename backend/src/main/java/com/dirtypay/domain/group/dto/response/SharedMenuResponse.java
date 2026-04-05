package com.dirtypay.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 그룹 공유 메뉴 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class SharedMenuResponse {

    private Long menuId;
    private String menuName;
    private BigDecimal price;
    private int quantity;
}
