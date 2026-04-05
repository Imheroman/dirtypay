package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreReviewCreateRequest;
import com.dirtypay.domain.store.dto.response.StoreReviewResponse;
import com.dirtypay.domain.store.entity.StoreReview;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.domain.store.repository.StoreReviewRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 매장 리뷰 비즈니스 로직 서비스.
 *
 * <p>매장 리뷰의 작성·수정·삭제·조회 기능을 제공한다.
 * 리뷰 수정·삭제는 작성자 본인만 가능하다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreReviewService {

    private final StoreRepository storeRepository;
    private final StoreReviewRepository storeReviewRepository;

    /**
     * 매장에 리뷰를 작성한다.
     *
     * <p>인증된 사용자라면 누구나 리뷰를 작성할 수 있다.</p>
     *
     * @param storeId  매장 ID
     * @param memberId 작성자 회원 ID
     * @param request  리뷰 생성 요청 DTO
     * @return 생성된 리뷰 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    @Transactional
    public StoreReviewResponse createReview(Long storeId, Long memberId, StoreReviewCreateRequest request) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_NOT_FOUND));

        StoreReview review = StoreReview.builder()
                .storeId(storeId)
                .memberId(memberId)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        return StoreReviewResponse.from(storeReviewRepository.save(review));
    }

    /**
     * 매장 리뷰를 수정한다.
     *
     * <p>본인({@code memberId})이 작성한 리뷰만 수정 가능하다.</p>
     *
     * @param storeId   매장 ID
     * @param reviewId  리뷰 ID
     * @param memberId  요청자 회원 ID
     * @param request   리뷰 수정 요청 DTO
     * @return 수정된 리뷰 응답 DTO
     * @throws EntityNotFoundException 리뷰를 찾을 수 없는 경우
     * @throws BusinessException       작성자가 아닌 경우
     */
    @Transactional
    public StoreReviewResponse updateReview(Long storeId, Long reviewId, Long memberId,
                                            StoreReviewCreateRequest request) {
        StoreReview review = storeReviewRepository.findByIdAndStoreId(reviewId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_REVIEW_NOT_FOUND));

        verifyReviewAuthor(review, memberId);
        review.update(request.getRating(), request.getContent());

        return StoreReviewResponse.from(review);
    }

    /**
     * 매장 리뷰를 삭제(Soft Delete)한다.
     *
     * <p>본인({@code memberId})이 작성한 리뷰 또는 매장 소유자만 삭제 가능하다.
     * 본인 확인은 {@code memberId} 일치 여부로 판단하며,
     * 매장 소유자 삭제 권한은 {@link StoreOwner} AOP를 통해 별도 엔드포인트에서 처리한다.</p>
     *
     * @param storeId  매장 ID
     * @param reviewId 리뷰 ID
     * @param memberId 요청자 회원 ID
     * @throws EntityNotFoundException 리뷰를 찾을 수 없는 경우
     * @throws BusinessException       작성자가 아닌 경우
     */
    @Transactional
    public void deleteReview(Long storeId, Long reviewId, Long memberId) {
        StoreReview review = storeReviewRepository.findByIdAndStoreId(reviewId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_REVIEW_NOT_FOUND));

        verifyReviewAuthor(review, memberId);
        review.delete();
    }

    /**
     * 매장의 전체 리뷰 목록을 최신순으로 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 리뷰 응답 DTO 목록 (최신순)
     */
    public List<StoreReviewResponse> getReviews(Long storeId) {
        return storeReviewRepository.findAllByStoreIdOrderByCreatedDateDesc(storeId).stream()
                .map(StoreReviewResponse::from)
                .toList();
    }

    /**
     * 특정 리뷰 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId  매장 ID
     * @param reviewId 리뷰 ID
     * @return 리뷰 응답 DTO
     * @throws EntityNotFoundException 리뷰를 찾을 수 없는 경우
     */
    public StoreReviewResponse getReview(Long storeId, Long reviewId) {
        StoreReview review = storeReviewRepository.findByIdAndStoreId(reviewId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_REVIEW_NOT_FOUND));
        return StoreReviewResponse.from(review);
    }

    /**
     * 매장의 평균 별점을 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다. 리뷰가 없으면 0.0을 반환한다.</p>
     *
     * @param storeId 매장 ID
     * @return 평균 별점
     */
    public Double getAverageRating(Long storeId) {
        return storeReviewRepository.findAverageRatingByStoreId(storeId);
    }

    /**
     * 리뷰 작성자를 검증한다.
     *
     * <p>요청자({@code memberId})가 리뷰 작성자인지 확인한다.
     * 작성자가 아닌 경우 {@code BusinessException}을 발생시킨다.</p>
     *
     * @param review   검증할 리뷰 엔티티
     * @param memberId 요청자 회원 ID
     * @throws BusinessException 요청자가 리뷰 작성자가 아닌 경우
     * @author kim-young-woong
     * @since 1.0.0
     */
    private void verifyReviewAuthor(StoreReview review, Long memberId) {
        if (!review.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.STORE_ACCESS_DENIED);
        }
    }
}
