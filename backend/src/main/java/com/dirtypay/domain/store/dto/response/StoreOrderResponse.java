package com.dirtypay.domain.store.dto.response;

import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 매장 주문 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreOrderResponse {

    /** 주문 ID. */
    private Long id;

    /** 소속 매장 ID. */
    private Long storeId;

    /** 주문 메뉴 ID. */
    private Long menuId;

    /** 주문 수량. */
    private int quantity;

    /** 총 금액. */
    private BigDecimal totalPrice;

    /** 주문 처리 상태. */
    private StoreOrderStatus status;

    /** 주문 번호 (UUID). */
    private String orderNumber;

    /** 주문자 이름. */
    private String customerName;

    /** 주문자 전화번호. */
    private String customerPhone;

    /** 주문자 회원 ID. 비회원 주문 시 null. */
    private Long memberId;

    /** 주문 시점 메뉴 단가. */
    private BigDecimal unitPrice;

    /** 주문 시점 메뉴명. */
    private String menuName;

    /** 생성 일시. */
    private LocalDateTime createdDate;

    /**
     * StoreOrder 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param order 매장 주문 엔티티
     * @return 매장 주문 응답 DTO
     */
    public static StoreOrderResponse from(StoreOrder order) {
        return StoreOrderResponse.builder()
                .id(order.getId())
                .storeId(order.getStoreId())
                .menuId(order.getMenuId())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .memberId(order.getMemberId())
                .unitPrice(order.getUnitPrice())
                .menuName(order.getMenuName())
                .createdDate(order.getCreatedDate())
                .build();
    }
}
