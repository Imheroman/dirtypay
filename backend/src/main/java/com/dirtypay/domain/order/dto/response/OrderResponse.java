package com.dirtypay.domain.order.dto.response;

import com.dirtypay.domain.order.entity.Order;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrderResponse {

    private Long id;
    private Long roundId;
    private Long groupId;
    private String groupName;
    private Long menuId;
    private String menuName;
    private BigDecimal menuPrice;
    private int quantity;
    private BigDecimal totalPrice;
    private List<OrderDetailResponse> details;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Order 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param order   주문 엔티티
     * @param details 주문 상세 응답 목록
     * @return 주문 응답 DTO
     */
    public static OrderResponse from(Order order, List<OrderDetailResponse> details) {
        return OrderResponse.builder()
                .id(order.getId())
                .roundId(order.getRoundId())
                .groupId(order.getGroupId())
                .groupName(order.getGroupName())
                .menuId(order.getMenuId())
                .menuName(order.getMenuName())
                .menuPrice(order.getMenuPrice())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .details(details)
                .createdDate(order.getCreatedDate())
                .updatedDate(order.getUpdatedDate())
                .build();
    }
}
