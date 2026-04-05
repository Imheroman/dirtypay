package com.dirtypay.domain.store.repository;

import java.math.BigDecimal;

/**
 * 인기 메뉴 집계 결과 Projection.
 *
 * <p>JPQL 집계 쿼리 결과를 타입 안전하게 매핑한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface PopularMenuProjection {

    /** 메뉴 ID. */
    Long getMenuId();

    /** 메뉴명. */
    String getMenuName();

    /** 주문 건수. */
    Long getOrderCount();

    /** 총 매출액. */
    BigDecimal getRevenue();
}
