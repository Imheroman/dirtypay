package com.dirtypay.domain.store.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.store.dto.request.StoreReviewCreateRequest;
import com.dirtypay.domain.store.dto.response.StoreReviewResponse;
import com.dirtypay.domain.store.service.StoreReviewService;
import com.dirtypay.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 매장 리뷰 REST API 컨트롤러.
 *
 * <p>매장 리뷰 작성·수정·삭제·조회 API를 제공한다.
 * GET 요청은 비로그인 사용자도 가능하며,
 * 리뷰 작성·수정·삭제는 JWT 인증이 필요하다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/stores/{storeId}/reviews")
@RequiredArgsConstructor
public class StoreReviewController {

    private final StoreReviewService storeReviewService;

    /**
     * 매장에 리뷰를 작성한다.
     *
     * <p>인증된 사용자라면 누구나 작성 가능하다.</p>
     *
     * @param storeId       매장 ID
     * @param userPrincipal 인증된 사용자 정보
     * @param request       리뷰 생성 요청 DTO
     * @return 201 Created + 생성된 리뷰 응답 DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreReviewResponse>> createReview(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid StoreReviewCreateRequest request) {
        StoreReviewResponse response = storeReviewService.createReview(storeId, userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 매장의 전체 리뷰 목록을 최신순으로 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 200 OK + 리뷰 응답 DTO 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreReviewResponse>>> getReviews(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.success(storeReviewService.getReviews(storeId)));
    }

    /**
     * 매장 리뷰 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId  매장 ID
     * @param reviewId 리뷰 ID
     * @return 200 OK + 리뷰 응답 DTO
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<StoreReviewResponse>> getReview(
            @PathVariable Long storeId,
            @PathVariable Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success(storeReviewService.getReview(storeId, reviewId)));
    }

    /**
     * 매장 리뷰를 수정한다.
     *
     * <p>본인이 작성한 리뷰만 수정 가능하다.</p>
     *
     * @param storeId       매장 ID
     * @param reviewId      리뷰 ID
     * @param userPrincipal 인증된 사용자 정보
     * @param request       리뷰 수정 요청 DTO
     * @return 200 OK + 수정된 리뷰 응답 DTO
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<StoreReviewResponse>> updateReview(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid StoreReviewCreateRequest request) {
        StoreReviewResponse response = storeReviewService.updateReview(
                storeId, reviewId, userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 매장 리뷰를 삭제(Soft Delete)한다.
     *
     * <p>본인이 작성한 리뷰만 삭제 가능하다.</p>
     *
     * @param storeId       매장 ID
     * @param reviewId      리뷰 ID
     * @param userPrincipal 인증된 사용자 정보
     * @return 200 OK
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        storeReviewService.deleteReview(storeId, reviewId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 매장의 평균 별점을 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 200 OK + 평균 별점
     */
    @GetMapping("/rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.success(storeReviewService.getAverageRating(storeId)));
    }
}
