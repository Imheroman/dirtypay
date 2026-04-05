package com.dirtypay.domain.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 인기 메뉴 목록 응답 DTO.
 *
 * <p>매장 내 주문 건수 기준으로 정렬된 인기 메뉴 목록을 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class PopularMenuResponse {

    /** 인기 메뉴 항목 목록. */
    private List<MenuItem> menus;

    /**
     * 인기 메뉴 단일 항목.
     *
     * @author kim-young-woong
     * @since 1.0.0
     */
    @Getter
    @Builder
    public static class MenuItem {

        /** 메뉴 ID. */
        private Long menuId;

        /** 메뉴명. */
        private String menuName;

        /** 주문 건수. */
        private Long orderCount;

        /** 총 매출액. */
        private BigDecimal revenue;
    }
}
