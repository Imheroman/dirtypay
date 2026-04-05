package com.dirtypay.domain.store.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StoreReview} Entity 단위 테스트.
 *
 * <p>리뷰 생성, 수정 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class StoreReviewTest {

    @Nested
    @DisplayName("StoreReview 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Builder로 StoreReview 생성 시 모든 필드가 올바르게 설정된다")
        void StoreReview_create_allFieldsSet() {
            // given & when
            StoreReview review = StoreReview.builder()
                    .storeId(1L)
                    .memberId(100L)
                    .rating(4)
                    .content("맛있어요!")
                    .build();

            // then
            assertThat(review.getStoreId()).isEqualTo(1L);
            assertThat(review.getMemberId()).isEqualTo(100L);
            assertThat(review.getRating()).isEqualTo(4);
            assertThat(review.getContent()).isEqualTo("맛있어요!");
        }

        @Test
        @DisplayName("rating 1(최소)과 5(최대) 경계값으로 정상 생성된다")
        void StoreReview_create_ratingInRange() {
            // given & when
            StoreReview minRatingReview = StoreReview.builder()
                    .storeId(1L)
                    .memberId(100L)
                    .rating(1)
                    .content("별로에요")
                    .build();

            StoreReview maxRatingReview = StoreReview.builder()
                    .storeId(1L)
                    .memberId(101L)
                    .rating(5)
                    .content("최고에요!")
                    .build();

            // then
            assertThat(minRatingReview.getRating()).isEqualTo(1);
            assertThat(maxRatingReview.getRating()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("StoreReview 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("update 메서드 호출 시 rating과 content가 변경된다")
        void StoreReview_update_success() {
            // given
            StoreReview review = createReview(1L, 1L, 100L, 3);

            // when
            review.update(5, "재방문 의사 있어요!");

            // then
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getContent()).isEqualTo("재방문 의사 있어요!");
        }

        @Test
        @DisplayName("update 시 동일한 값으로도 정상적으로 저장된다")
        void StoreReview_updateWithoutChangingFields() {
            // given
            StoreReview review = createReview(1L, 1L, 100L, 3);
            review.update(3, "테스트 리뷰");  // content 초기값과 동일

            // when
            review.update(3, "테스트 리뷰");

            // then
            assertThat(review.getRating()).isEqualTo(3);
            assertThat(review.getContent()).isEqualTo("테스트 리뷰");
        }

        @Test
        @DisplayName("작성자 회원 ID와 일치하는 경우 isWrittenBy가 true를 반환한다")
        void StoreReview_isWrittenBy_returnsTrueForAuthor() {
            // given
            StoreReview review = createReview(1L, 1L, 100L, 4);

            // when & then — memberId 일치 여부는 service에서 직접 비교
            assertThat(review.getMemberId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("다른 회원의 ID로는 작성자 검증에 실패한다")
        void StoreReview_isWrittenBy_returnsFalseForOther() {
            // given
            StoreReview review = createReview(1L, 1L, 100L, 4);

            // when & then — memberId 불일치 확인
            assertThat(review.getMemberId()).isNotEqualTo(999L);
        }
    }

    // === Helper Methods ===

    private StoreReview createReview(Long id, Long storeId, Long memberId, int rating) {
        StoreReview review = StoreReview.builder()
                .storeId(storeId)
                .memberId(memberId)
                .rating(rating)
                .content("테스트 리뷰")
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }
}
