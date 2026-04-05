package com.dirtypay.domain.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 매장 통계 응답 DTO.
 *
 * <p>기본 조회 기간은 최근 30일이며, 일 평균 주문 수·총 주문 건수·총 매출액을 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreStatisticsResponse {

    /** 매장 ID. */
    private Long storeId;

    /** 일 평균 주문 수 (소수점 2자리, RoundingMode.HALF_UP 적용). */
    private BigDecimal averageDailyOrders;

    /** 총 주문 건수. */
    private Long totalOrders;

    /** 총 매출액. */
    private BigDecimal totalRevenue;

    /** 조회 시작일 (기본 30일 전). */
    private LocalDate periodStart;

    /** 조회 종료일 (기본 오늘). */
    private LocalDate periodEnd;
}
