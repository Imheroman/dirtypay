package com.dirtypay.domain.store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 리뷰 생성 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreReviewCreateRequest {

    /**
     * 별점. 필수, 1~5 범위.
     */
    @NotNull(message = "별점은 필수입니다")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5 이하이어야 합니다")
    private Integer rating;

    /**
     * 리뷰 내용. 선택, 최대 1000자.
     */
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자입니다")
    private String content;
}
