package com.dirtypay.domain.order.dto.response;

import com.dirtypay.domain.order.entity.OrderDetail;
import lombok.Builder;
import lombok.Getter;

/**
 * 주문 상세 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderDetailResponse {

    private Long id;
    private Long orderId;
    private Long orgMemberId;
    private String nickname;
    private int shareRatio;

    /**
     * OrderDetail 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param detail   주문 상세 엔티티
     * @param nickname 멤버 닉네임
     * @return 주문 상세 응답 DTO
     */
    public static OrderDetailResponse from(OrderDetail detail, String nickname) {
        return OrderDetailResponse.builder()
                .id(detail.getId())
                .orderId(detail.getOrderId())
                .orgMemberId(detail.getOrgMemberId())
                .nickname(nickname)
                .shareRatio(detail.getShareRatio())
                .build();
    }
}
