package com.dirtypay.domain.settlement.dto.response;

import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 그룹별 정산 결과 응답 DTO.
 *
 * <p>특정 그룹의 주문만을 대상으로 멤버별 정산 금액을 계산한 결과를 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeSettlementResponse {

    /** 그룹 ID. */
    private Long groupId;

    /** 그룹 이름. */
    private String groupName;

    /** 노드 내 전체 주문 합산 금액. */
    private BigDecimal totalAmount;

    /** 나머지 금액 처리 전략. */
    private RemainderStrategyType strategy;

    /** 멤버별 정산 금액 목록. */
    private List<MemberAmountResponse> settlements;
}
