package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreReviewCreateRequest;
import com.dirtypay.domain.store.dto.response.StoreReviewResponse;
import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreReview;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.domain.store.repository.StoreReviewRepository;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * {@link StoreReviewService} 단위 테스트.
 *
 * <p>리뷰 작성, 수정, 삭제(작성자 검증), 조회 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StoreReviewServiceTest {

    @InjectMocks
    private StoreReviewService storeReviewService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreReviewRepository storeReviewRepository;

    @Nested
    @DisplayName("리뷰 작성 테스트")
    class CreateReviewTest {

        @Test
        @DisplayName("유효한 storeId와 memberId로 리뷰 작성 성공 시 StoreReviewResponse를 반환한다")
        void StoreReviewService_createReview_success() {
            // given
            Long storeId = 1L;
            Long memberId = 100L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreReviewCreateRequest request = createReviewRequest(4, "맛있어요!");
            StoreReview savedReview = createReview(10L, storeId, memberId, 4);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeReviewRepository.save(any(StoreReview.class))).willReturn(savedReview);

            // when
            StoreReviewResponse response = storeReviewService.createReview(storeId, memberId, request);

            // then
            assertThat(response.getStoreId()).isEqualTo(storeId);
            assertThat(response.getMemberId()).isEqualTo(memberId);
            assertThat(response.getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("존재하지 않는 storeId로 리뷰 작성 시 EntityNotFoundException이 발생한다")
        void StoreReviewService_createReview_storeNotFound_throwsException() {
            // given
            Long nonExistentStoreId = 999L;
            Long memberId = 100L;
            StoreReviewCreateRequest request = createReviewRequest(4, "맛있어요!");

            given(storeRepository.findById(nonExistentStoreId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeReviewService.createReview(nonExistentStoreId, memberId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("rating 경계값(1, 5)으로 리뷰 작성이 성공한다")
        void StoreReviewService_createReview_ratingBoundary() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreReview minReview = createReview(1L, storeId, 100L, 1);
            StoreReview maxReview = createReview(2L, storeId, 101L, 5);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeReviewRepository.save(any(StoreReview.class)))
                    .willReturn(minReview)
                    .willReturn(maxReview);

            // when
            StoreReviewResponse minResponse = storeReviewService.createReview(storeId, 100L,
                    createReviewRequest(1, "별로에요"));
            StoreReviewResponse maxResponse = storeReviewService.createReview(storeId, 101L,
                    createReviewRequest(5, "최고에요!"));

            // then
            assertThat(minResponse.getRating()).isEqualTo(1);
            assertThat(maxResponse.getRating()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class GetReviewsTest {

        @Test
        @DisplayName("getReviews: 매장의 전체 리뷰를 최신순으로 반환한다")
        void StoreReviewService_getReviews_sortByLatest() {
            // given
            Long storeId = 1L;
            List<StoreReview> reviews = List.of(
                    createReview(2L, storeId, 101L, 3),
                    createReview(1L, storeId, 100L, 5)
            );

            given(storeReviewRepository.findAllByStoreIdOrderByCreatedDateDesc(storeId))
                    .willReturn(reviews);

            // when
            List<StoreReviewResponse> responses = storeReviewService.getReviews(storeId);

            // then
            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("getReviews: 리뷰가 없는 매장의 경우 빈 리스트를 반환한다")
        void StoreReviewService_getReviews_emptyList() {
            // given
            Long storeId = 1L;
            given(storeReviewRepository.findAllByStoreIdOrderByCreatedDateDesc(storeId))
                    .willReturn(List.of());

            // when
            List<StoreReviewResponse> responses = storeReviewService.getReviews(storeId);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 테스트")
    class DeleteReviewTest {

        @Test
        @DisplayName("작성자 본인이 리뷰 삭제 성공 시 deletedDate가 설정된다")
        void StoreReviewService_deleteReview_success() {
            // given
            Long storeId = 1L;
            Long reviewId = 10L;
            Long memberId = 100L;
            StoreReview review = createReview(reviewId, storeId, memberId, 4);

            given(storeReviewRepository.findByIdAndStoreId(reviewId, storeId))
                    .willReturn(Optional.of(review));

            // when
            storeReviewService.deleteReview(storeId, reviewId, memberId);

            // then
            assertThat(review.getDeletedDate()).isNotNull();
        }

        @Test
        @DisplayName("다른 회원이 리뷰 삭제 시도 시 BusinessException이 발생한다")
        void StoreReviewService_deleteReview_failWhenWrongMember() {
            // given
            Long storeId = 1L;
            Long reviewId = 10L;
            Long authorId = 100L;
            Long otherMemberId = 999L;
            StoreReview review = createReview(reviewId, storeId, authorId, 4);

            given(storeReviewRepository.findByIdAndStoreId(reviewId, storeId))
                    .willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> storeReviewService.deleteReview(storeId, reviewId, otherMemberId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 reviewId 삭제 시 EntityNotFoundException이 발생한다")
        void StoreReviewService_deleteReview_notFound_throwsException() {
            // given
            Long storeId = 1L;
            Long nonExistentReviewId = 999L;
            Long memberId = 100L;

            given(storeReviewRepository.findByIdAndStoreId(nonExistentReviewId, storeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeReviewService.deleteReview(storeId, nonExistentReviewId, memberId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("리뷰 수정 테스트")
    class UpdateReviewTest {

        @Test
        @DisplayName("작성자 본인이 리뷰 수정 성공 시 변경된 StoreReviewResponse를 반환한다")
        void StoreReviewService_updateReview_success() {
            // given
            Long storeId = 1L;
            Long reviewId = 10L;
            Long memberId = 100L;
            StoreReview review = createReview(reviewId, storeId, memberId, 3);
            StoreReviewCreateRequest request = createReviewRequest(5, "재방문 의사 있어요!");

            given(storeReviewRepository.findByIdAndStoreId(reviewId, storeId))
                    .willReturn(Optional.of(review));

            // when
            StoreReviewResponse response = storeReviewService.updateReview(storeId, reviewId, memberId, request);

            // then
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getContent()).isEqualTo("재방문 의사 있어요!");
        }

        @Test
        @DisplayName("다른 회원이 리뷰 수정 시도 시 BusinessException이 발생한다")
        void StoreReviewService_updateReview_failWhenWrongMember() {
            // given
            Long storeId = 1L;
            Long reviewId = 10L;
            Long authorId = 100L;
            Long otherMemberId = 999L;
            StoreReview review = createReview(reviewId, storeId, authorId, 3);
            StoreReviewCreateRequest request = createReviewRequest(5, "수정 내용");

            given(storeReviewRepository.findByIdAndStoreId(reviewId, storeId))
                    .willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> storeReviewService.updateReview(storeId, reviewId, otherMemberId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // === Helper Methods ===

    private Store createStore(Long id, Long ownerId, String name) {
        Store store = Store.builder()
                .ownerId(ownerId)
                .name(name)
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

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

    private StoreReviewCreateRequest createReviewRequest(int rating, String content) {
        StoreReviewCreateRequest request = new StoreReviewCreateRequest();
        ReflectionTestUtils.setField(request, "rating", rating);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
