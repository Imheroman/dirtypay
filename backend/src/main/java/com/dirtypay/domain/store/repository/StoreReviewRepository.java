package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.StoreReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 매장 리뷰 리포지토리.
 *
 * <p>{@code @SQLRestriction("deleted_date IS NULL")}에 의해 삭제된 엔티티가
 * 모든 쿼리에서 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface StoreReviewRepository extends JpaRepository<StoreReview, Long> {

    /**
     * 매장 ID에 속한 전체 리뷰를 생성일시 내림차순으로 조회한다.
     *
     * @param storeId 매장 ID
     * @return 리뷰 목록 (최신순)
     */
    List<StoreReview> findAllByStoreIdOrderByCreatedDateDesc(Long storeId);

    /**
     * 리뷰 ID와 매장 ID로 리뷰를 조회한다.
     *
     * <p>리뷰가 해당 매장에 속하는지 함께 검증할 때 사용한다.</p>
     *
     * @param id      리뷰 ID
     * @param storeId 매장 ID
     * @return 리뷰 Optional
     */
    Optional<StoreReview> findByIdAndStoreId(Long id, Long storeId);

    /**
     * 매장 ID와 회원 ID로 리뷰 목록을 조회한다.
     *
     * <p>특정 회원이 해당 매장에 작성한 리뷰 목록을 조회한다.</p>
     *
     * @param storeId  매장 ID
     * @param memberId 회원 ID
     * @return 해당 회원이 작성한 리뷰 목록
     */
    List<StoreReview> findAllByStoreIdAndMemberId(Long storeId, Long memberId);

    /**
     * 매장의 평균 별점을 조회한다.
     *
     * <p>리뷰가 없으면 0.0을 반환한다.</p>
     *
     * @param storeId 매장 ID
     * @return 평균 별점
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM StoreReview r WHERE r.storeId = :storeId")
    Double findAverageRatingByStoreId(@Param("storeId") Long storeId);
}
