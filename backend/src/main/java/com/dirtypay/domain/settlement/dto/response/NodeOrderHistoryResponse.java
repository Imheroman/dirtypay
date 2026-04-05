package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 노드별 개별 주문 기록 응답 DTO.
 *
 * <p>단일 주문에 대한 단가·수량·합계 및 참여자 닉네임 목록을 포함한다.</p>
 * <p>예시: "3.8. 오후 07:39  15,000원 × 6  [gg]"</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeOrderHistoryResponse {

    /** 주문 ID. */
    private Long orderId;

    /** 메뉴 단가. */
    private BigDecimal menuPrice;

    /** 주문 수량. */
    private int quantity;

    /** 단가 × 수량 합계 금액. */
    private BigDecimal totalPrice;

    /** 주문에 참여한 멤버 닉네임 목록. */
    private List<String> memberNicknames;

    /** 주문 생성 시각. */
    private LocalDateTime createdDate;
}
