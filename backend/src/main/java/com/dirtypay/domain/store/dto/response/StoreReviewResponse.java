package com.dirtypay.domain.store.dto.response;

import com.dirtypay.domain.store.entity.StoreReview;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 매장 리뷰 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreReviewResponse {

    /** 리뷰 ID. */
    private Long id;

    /** 리뷰 대상 매장 ID. */
    private Long storeId;

    /** 작성자 회원 ID. */
    private Long memberId;

    /** 별점 (1~5). */
    private int rating;

    /** 리뷰 내용. */
    private String content;

    /** 생성 일시. */
    private LocalDateTime createdDate;

    /**
     * StoreReview 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param review 매장 리뷰 엔티티
     * @return 매장 리뷰 응답 DTO
     */
    public static StoreReviewResponse from(StoreReview review) {
        return StoreReviewResponse.builder()
                .id(review.getId())
                .storeId(review.getStoreId())
                .memberId(review.getMemberId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdDate(review.getCreatedDate())
                .build();
    }
}
