package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 특정 그룹의 전체 주문 내역 응답 DTO.
 *
 * <p>그룹 식별 정보, 전체 주문 합산 금액, 카테고리별 그룹화된 주문 목록을 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeOrdersResponse {

    /** 그룹 ID. */
    private Long groupId;

    /** 그룹 이름. */
    private String groupName;

    /** 노드 내 전체 주문 합산 금액. */
    private BigDecimal totalAmount;

    /** 카테고리별 주문 그룹 목록. */
    private List<NodeCategoryGroupResponse> categories;
}
