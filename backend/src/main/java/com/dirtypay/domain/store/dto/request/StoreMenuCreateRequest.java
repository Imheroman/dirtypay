package com.dirtypay.domain.store.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 매장 메뉴 생성 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreMenuCreateRequest {

    /**
     * 메뉴명. 필수, 최대 100자.
     */
    @NotBlank(message = "메뉴명은 필수입니다")
    @Size(max = 100, message = "메뉴명은 최대 100자입니다")
    private String name;

    /**
     * 메뉴 설명. 선택, 최대 500자.
     */
    @Size(max = 500, message = "메뉴 설명은 최대 500자입니다")
    private String description;

    /**
     * 메뉴 가격. 필수, 0 초과.
     */
    @NotNull(message = "메뉴 가격은 필수입니다")
    @DecimalMin(value = "0.01", message = "메뉴 가격은 0보다 커야 합니다")
    private BigDecimal price;

    /**
     * 카테고리. 선택, 최대 50자.
     */
    @Size(max = 50, message = "카테고리는 최대 50자입니다")
    private String category;

    /**
     * 이미지 URL. 선택, 최대 500자.
     */
    @Size(max = 500, message = "이미지 URL은 최대 500자입니다")
    private String imageUrl;

    /**
     * 판매 가능 여부. 필수.
     */
    @NotNull(message = "판매 가능 여부는 필수입니다")
    private Boolean available;

    /**
     * 노출 순서. 0 이상.
     */
    @Min(value = 0, message = "노출 순서는 0 이상이어야 합니다")
    private int sortOrder;
}
