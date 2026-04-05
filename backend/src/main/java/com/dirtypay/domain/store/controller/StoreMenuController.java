package com.dirtypay.domain.store.controller;

import com.dirtypay.domain.store.dto.request.StoreMenuCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreMenuUpdateRequest;
import com.dirtypay.domain.store.dto.response.StoreMenuResponse;
import com.dirtypay.domain.store.service.StoreMenuService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.StoreOwner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 매장 메뉴 REST API 컨트롤러.
 *
 * <p>매장 메뉴 등록·수정·삭제·조회·판매가능여부 토글 API를 제공한다.
 * GET 요청은 비로그인 사용자도 접근 가능하며,
 * 쓰기 작업은 {@link StoreOwner} AOP로 소유권을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/stores/{storeId}/menus")
@RequiredArgsConstructor
public class StoreMenuController {

    private final StoreMenuService storeMenuService;

    /**
     * 매장에 새로운 메뉴를 추가한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param request 메뉴 생성 요청 DTO
     * @return 201 Created + 생성된 메뉴 응답 DTO
     */
    @PostMapping
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreMenuResponse>> createMenu(
            @PathVariable Long storeId,
            @RequestBody @Valid StoreMenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(storeMenuService.createMenu(storeId, request)));
    }

    /**
     * 매장 메뉴 목록을 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 200 OK + 메뉴 응답 DTO 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreMenuResponse>>> getMenus(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.success(storeMenuService.getMenus(storeId)));
    }

    /**
     * 매장의 판매 가능한 메뉴 목록을 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 200 OK + 판매 가능 메뉴 응답 DTO 목록 (sortOrder 오름차순)
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<StoreMenuResponse>>> getAvailableMenus(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.success(storeMenuService.getAvailableMenus(storeId)));
    }

    /**
     * 특정 메뉴 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @return 200 OK + 메뉴 응답 DTO
     */
    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<StoreMenuResponse>> getMenu(
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(storeMenuService.getMenu(storeId, menuId)));
    }

    /**
     * 매장 메뉴 정보를 수정한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @param request 메뉴 수정 요청 DTO
     * @return 200 OK + 수정된 메뉴 응답 DTO
     */
    @PutMapping("/{menuId}")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreMenuResponse>> updateMenu(
            @PathVariable Long storeId,
            @PathVariable Long menuId,
            @RequestBody @Valid StoreMenuUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(storeMenuService.updateMenu(storeId, menuId, request)));
    }

    /**
     * 메뉴 판매 가능 여부를 토글한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @return 200 OK + 토글 후 메뉴 응답 DTO
     */
    @PatchMapping("/{menuId}/toggle")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreMenuResponse>> toggleAvailability(
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        return ResponseEntity.ok(ApiResponse.success(storeMenuService.toggleAvailability(storeId, menuId)));
    }

    /**
     * 매장 메뉴를 삭제(Soft Delete)한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @return 200 OK
     */
    @DeleteMapping("/{menuId}")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @PathVariable Long storeId,
            @PathVariable Long menuId) {
        storeMenuService.deleteMenu(storeId, menuId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
