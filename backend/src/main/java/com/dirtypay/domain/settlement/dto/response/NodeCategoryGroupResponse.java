package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 노드별 카테고리 그룹 응답 DTO.
 *
 * <p>동일 카테고리에 속하는 메뉴 요약 목록과 카테고리 합산 금액을 포함한다.</p>
 * <p>예시: "기타 총 275,000원"</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeCategoryGroupResponse {

    /** 카테고리 이름. */
    private String category;

    /** 카테고리 내 전체 주문 합산 금액. */
    private BigDecimal totalAmount;

    /** 카테고리 내 메뉴 요약 목록. */
    private List<NodeMenuSummaryResponse> menus;
}
