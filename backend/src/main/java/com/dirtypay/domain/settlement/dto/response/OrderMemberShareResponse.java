package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 주문별 정산 멤버 지분 응답 DTO.
 *
 * <p>각 메뉴 항목에 대한 개별 멤버의 분담 지분을 나타낸다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderMemberShareResponse {

    private Long orgMemberId;
    private String nickname;
    private int shareRatio;
    private int totalRatio;
    private BigDecimal amount;
}
