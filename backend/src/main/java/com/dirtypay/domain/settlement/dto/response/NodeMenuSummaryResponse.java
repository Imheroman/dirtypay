package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 노드별 동일 메뉴 주문 합산 응답 DTO.
 *
 * <p>같은 메뉴에 대한 전체 주문 금액 합산, 참여자 요약, 개별 주문 이력을 포함한다.</p>
 * <p>예시: "삼겹살 240,000원 [gg ×2]"</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeMenuSummaryResponse {

    /** 메뉴 ID. */
    private Long menuId;

    /** 메뉴 이름. */
    private String menuName;

    /** 해당 메뉴 전체 주문 합산 금액. */
    private BigDecimal totalPrice;

    /** 해당 메뉴의 총 주문 건수. */
    private int orderCount;

    /** 참여자별 주문 횟수 요약 목록. */
    private List<NodeMemberCountResponse> members;

    /** 개별 주문 이력 목록. */
    private List<NodeOrderHistoryResponse> orders;
}
