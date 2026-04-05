package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreReview;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StoreReviewRepository} 통합 테스트.
 *
 * <p>리뷰 조회, 회원별 필터링, AVG 집계 쿼리, Soft Delete 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class StoreReviewRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreReviewRepository storeReviewRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private StoreReview review1;
    private StoreReview review2;
    private StoreReview review3;

    @BeforeEach
    void setUp() {
        storeReviewRepository.deleteAll();
        storeRepository.deleteAll();

        // 매장 생성
        store = storeRepository.save(Store.builder()
                .ownerId(1L)
                .name("테스트 매장")
                .businessNumber("111-11-11111")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .status(StoreStatus.ACTIVE)
                .build());

        // 리뷰 생성 (회원 100L: 2건, 회원 200L: 1건)
        review1 = storeReviewRepository.save(StoreReview.builder()
                .storeId(store.getId())
                .memberId(100L)
                .rating(5)
                .content("최고에요!")
                .build());

        review2 = storeReviewRepository.save(StoreReview.builder()
                .storeId(store.getId())
                .memberId(100L)
                .rating(3)
                .content("보통이에요")
                .build());

        review3 = storeReviewRepository.save(StoreReview.builder()
                .storeId(store.getId())
                .memberId(200L)
                .rating(4)
                .content("맛있어요")
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("storeId로 리뷰 조회 테스트")
    class FindByStoreIdTest {

        @Test
        @DisplayName("findAllByStoreIdOrderByCreatedDateDesc: 매장의 모든 리뷰를 최신순으로 반환한다")
        void StoreReviewRepository_findByStoreId_sortByCreatedDateDesc() {
            // when
            List<StoreReview> reviews = storeReviewRepository
                    .findAllByStoreIdOrderByCreatedDateDesc(store.getId());

            // then
            assertThat(reviews).hasSize(3);
        }

        @Test
        @DisplayName("findByIdAndStoreId: 올바른 reviewId + storeId 조합으로 리뷰를 조회한다")
        void StoreReviewRepository_save_success() {
            // when
            Optional<StoreReview> result = storeReviewRepository
                    .findByIdAndStoreId(review1.getId(), store.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("findByIdAndStoreId: storeId가 불일치하면 빈 Optional을 반환한다")
        void StoreReviewRepository_findByIdAndStoreId_throwsWhenNotFound() {
            // when
            Optional<StoreReview> result = storeReviewRepository
                    .findByIdAndStoreId(review1.getId(), 9999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원별 리뷰 조회 테스트")
    class FindByMemberIdTest {

        @Test
        @DisplayName("findAllByStoreIdAndMemberId: 특정 회원이 작성한 리뷰 목록을 반환한다")
        void StoreReviewRepository_findAllByStoreIdAndMemberId_returnsReviewsByMember() {
            // when
            List<StoreReview> member100Reviews = storeReviewRepository
                    .findAllByStoreIdAndMemberId(store.getId(), 100L);

            List<StoreReview> member200Reviews = storeReviewRepository
                    .findAllByStoreIdAndMemberId(store.getId(), 200L);

            // then
            assertThat(member100Reviews).hasSize(2);
            assertThat(member200Reviews).hasSize(1);
        }
    }

    @Nested
    @DisplayName("평균 별점 집계 테스트")
    class AvgRatingTest {

        @Test
        @DisplayName("findAverageRatingByStoreId: 매장의 평균 별점을 정확하게 계산한다 (5+3+4)/3 = 4.0")
        void StoreReviewRepository_avgRatingByStoreId_returnsAverage() {
            // when
            Double avgRating = storeReviewRepository.findAverageRatingByStoreId(store.getId());

            // then — (5 + 3 + 4) / 3 = 4.0
            assertThat(avgRating).isEqualTo(4.0);
        }

        @Test
        @DisplayName("findAverageRatingByStoreId: 리뷰가 없는 매장은 0.0을 반환한다")
        void StoreReviewRepository_avgRatingByStoreId_noReviews_returnsZero() {
            // given — 리뷰 없는 매장
            Store emptyStore = storeRepository.save(Store.builder()
                    .ownerId(2L)
                    .name("신규 매장")
                    .businessNumber("999-99-99999")
                    .address("부산시 해운대구")
                    .storeType(StoreType.DIRECT)
                    .status(StoreStatus.ACTIVE)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // when
            Double avgRating = storeReviewRepository.findAverageRatingByStoreId(emptyStore.getId());

            // then
            assertThat(avgRating).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("소프트 삭제 필터링 테스트")
    class SoftDeleteTest {

        @Test
        @DisplayName("@SQLRestriction: Soft Delete된 리뷰는 조회 및 집계에서 자동으로 제외된다")
        void StoreReviewRepository_sqlRestriction_excludesDeleted() {
            // given — review1 소프트 삭제
            StoreReview reviewToDelete = storeReviewRepository.findById(review1.getId()).get();
            reviewToDelete.delete();  // rating=5 삭제
            storeReviewRepository.save(reviewToDelete);
            entityManager.flush();
            entityManager.clear();

            // when
            List<StoreReview> reviews = storeReviewRepository
                    .findAllByStoreIdOrderByCreatedDateDesc(store.getId());
            Double avgRating = storeReviewRepository.findAverageRatingByStoreId(store.getId());

            // then — review1(5점) 제외 후 2건: (3+4)/2 = 3.5
            assertThat(reviews).hasSize(2);
            assertThat(avgRating).isEqualTo(3.5);
        }
    }
}
