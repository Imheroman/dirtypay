package com.dirtypay.domain.store.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.store.dto.request.StoreCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreStatusChangeRequest;
import com.dirtypay.domain.store.dto.request.StoreUpdateRequest;
import com.dirtypay.domain.store.dto.response.PopularMenuResponse;
import com.dirtypay.domain.store.dto.response.StoreResponse;
import com.dirtypay.domain.store.dto.response.StoreStatisticsResponse;
import com.dirtypay.domain.store.service.StoreService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.security.annotation.StoreOwner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 매장 REST API 컨트롤러.
 *
 * <p>매장 등록·수정·삭제·조회·통계 API를 제공한다.
 * GET 요청은 비로그인 사용자도 접근 가능하며,
 * 쓰기 작업은 {@link StoreOwner} AOP 또는 JWT 인증이 필요하다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /**
     * 새로운 매장을 등록한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param request       매장 등록 요청 DTO
     * @return 201 Created + 등록된 매장 응답 DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid StoreCreateRequest request) {
        StoreResponse response = storeService.createStore(userPrincipal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 매장 목록을 페이지 단위로 조회한다.
     *
     * <p>{@code scope} 파라미터로 조회 범위를 지정한다.
     * <ul>
     *   <li>{@code public} (기본값): 공개·활성 매장만 반환 (비로그인 허용)</li>
     *   <li>{@code my}: 인증 사용자 소유 전체 매장 반환 (인증 필수)</li>
     * </ul>
     * {@code public} 또는 {@code my} 외의 값을 전달하면 400 Bad Request가 반환된다.</p>
     *
     * @param scope         조회 범위 ({@code "public"} | {@code "my"}, 기본: {@code "public"})
     * @param userPrincipal 인증된 사용자 정보 (scope=my일 때 필수)
     * @param pageable      페이지 요청 정보 (기본: 20건, 생성일시 내림차순)
     * @return 200 OK + 매장 응답 DTO 페이지
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StoreResponse>>> getStores(
            @Pattern(regexp = "^(public|my)$", message = "scope는 'public' 또는 'my'만 허용됩니다.")
            @RequestParam(required = false, defaultValue = "public") String scope,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        if ("my".equals(scope)) {
            if (userPrincipal == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return ResponseEntity.ok(ApiResponse.success(
                    storeService.getMyStores(userPrincipal.getId(), pageable)));
        }
        return ResponseEntity.ok(ApiResponse.success(storeService.getStores(pageable)));
    }

    /**
     * 매장 상세 정보를 조회한다.
     *
     * <p>CUSTOM 매장은 소유자만 접근 가능하다.
     * 비로그인 사용자가 CUSTOM 매장을 조회하면 403이 반환된다.</p>
     *
     * @param storeId       매장 ID
     * @param userPrincipal 인증된 사용자 정보 (비로그인 시 null)
     * @return 200 OK + 매장 응답 DTO
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(
            @PathVariable Long storeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long requesterId = userPrincipal != null ? userPrincipal.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(storeService.getStore(storeId, requesterId)));
    }

    /**
     * 매장 정보를 수정한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param request 매장 수정 요청 DTO
     * @return 200 OK + 수정된 매장 응답 DTO
     */
    @PutMapping("/{storeId}")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable Long storeId,
            @RequestBody @Valid StoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeService.updateStore(storeId, request)));
    }

    /**
     * 매장 운영 상태를 변경한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param request 상태 변경 요청 DTO
     * @return 200 OK + 상태가 변경된 매장 응답 DTO
     */
    @PatchMapping("/{storeId}/status")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreResponse>> changeStatus(
            @PathVariable Long storeId,
            @RequestBody @Valid StoreStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeService.changeStatus(storeId, request)));
    }

    /**
     * 매장을 삭제(Soft Delete)한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @return 200 OK
     */
    @DeleteMapping("/{storeId}")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable Long storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 매장 통계를 조회한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.
     * 날짜 파라미터 미입력 시 최근 30일 기준으로 조회한다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작일 (yyyy-MM-dd, 선택)
     * @param endDate   조회 종료일 (yyyy-MM-dd, 선택)
     * @return 200 OK + 매장 통계 응답 DTO
     */
    @GetMapping("/{storeId}/statistics")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreStatisticsResponse>> getStatistics(
            @PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate resolvedEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStartDate = startDate != null ? startDate : resolvedEndDate.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(
                storeService.getStatistics(storeId, resolvedStartDate, resolvedEndDate)));
    }

    /**
     * 매장 인기 메뉴 목록을 조회한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.
     * 날짜 파라미터 미입력 시 최근 30일 기준으로 조회한다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작일 (yyyy-MM-dd, 선택)
     * @param endDate   조회 종료일 (yyyy-MM-dd, 선택)
     * @return 200 OK + 인기 메뉴 목록 응답 DTO
     */
    @GetMapping("/{storeId}/statistics/popular-menus")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<PopularMenuResponse>> getPopularMenus(
            @PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate resolvedEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStartDate = startDate != null ? startDate : resolvedEndDate.minusDays(30);
        return ResponseEntity.ok(ApiResponse.success(
                storeService.getPopularMenus(storeId, resolvedStartDate, resolvedEndDate)));
    }
}
