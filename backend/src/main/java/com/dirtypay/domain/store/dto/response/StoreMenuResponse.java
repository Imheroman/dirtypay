package com.dirtypay.domain.store.dto.response;

import com.dirtypay.domain.store.entity.StoreMenu;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 매장 메뉴 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreMenuResponse {

    /** 메뉴 ID. */
    private Long id;

    /** 소속 매장 ID. */
    private Long storeId;

    /** 메뉴명. */
    private String name;

    /** 메뉴 설명. */
    private String description;

    /** 가격. */
    private BigDecimal price;

    /** 카테고리. */
    private String category;

    /** 이미지 URL. */
    private String imageUrl;

    /** 판매 가능 여부. */
    private boolean available;

    /** 노출 순서. */
    private int sortOrder;

    /** 생성 일시. */
    private LocalDateTime createdDate;

    /**
     * StoreMenu 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param menu 매장 메뉴 엔티티
     * @return 매장 메뉴 응답 DTO
     */
    public static StoreMenuResponse from(StoreMenu menu) {
        return StoreMenuResponse.builder()
                .id(menu.getId())
                .storeId(menu.getStoreId())
                .name(menu.getName())
                .description(menu.getDescription())
                .price(menu.getPrice())
                .category(menu.getCategory())
                .imageUrl(menu.getImageUrl())
                .available(menu.isAvailable())
                .sortOrder(menu.getSortOrder())
                .createdDate(menu.getCreatedDate())
                .build();
    }
}
