package com.dirtypay.domain.store.controller;

import com.dirtypay.domain.store.dto.request.StoreOrderCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreOrderStatusChangeRequest;
import com.dirtypay.domain.store.dto.response.StoreOrderResponse;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import com.dirtypay.domain.store.service.StoreOrderService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.StoreOwner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장 주문 REST API 컨트롤러.
 *
 * <p>매장 주문 생성·상태 변경·취소·조회 API를 제공한다.
 * 주문 생성·단건 조회는 비로그인 사용자도 가능하며,
 * 주문 목록 조회·상태 변경·취소는 매장 소유자({@link StoreOwner} AOP) 권한이 필요하다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/stores/{storeId}/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final StoreOrderService storeOrderService;

    /**
     * 매장에 새로운 주문을 생성한다.
     *
     * <p>비로그인 사용자도 주문 생성이 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @param request 주문 생성 요청 DTO
     * @return 201 Created + 생성된 주문 응답 DTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StoreOrderResponse>> createOrder(
            @PathVariable Long storeId,
            @RequestBody @Valid StoreOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(storeOrderService.createOrder(storeId, request)));
    }

    /**
     * 매장의 주문 목록을 페이지 단위로 최신순 조회한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId  매장 ID
     * @param status   주문 상태 필터 (선택)
     * @param pageable 페이지 요청 정보
     * @return 200 OK + 주문 응답 DTO 페이지
     */
    @GetMapping
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<Page<StoreOrderResponse>>> getOrders(
            @PathVariable Long storeId,
            @RequestParam(required = false) StoreOrderStatus status,
            Pageable pageable) {
        Page<StoreOrderResponse> orders = status != null
                ? storeOrderService.getOrdersByStatus(storeId, status, pageable)
                : storeOrderService.getOrders(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 매장 주문 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @return 200 OK + 주문 응답 DTO
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<StoreOrderResponse>> getOrder(
            @PathVariable Long storeId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(storeOrderService.getOrder(storeId, orderId)));
    }

    /**
     * 매장 주문 상태를 변경한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @param request 주문 상태 변경 요청 DTO
     * @return 200 OK + 상태가 변경된 주문 응답 DTO
     */
    @PatchMapping("/{orderId}/status")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<StoreOrderResponse>> changeOrderStatus(
            @PathVariable Long storeId,
            @PathVariable Long orderId,
            @RequestBody @Valid StoreOrderStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                storeOrderService.changeOrderStatus(storeId, orderId, request)));
    }

    /**
     * 매장 주문을 취소한다.
     *
     * <p>{@link StoreOwner} AOP에서 소유권을 검증한다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @return 200 OK
     */
    @DeleteMapping("/{orderId}")
    @StoreOwner(value = "storeId", resourceType = StoreOwner.ResourceType.STORE)
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long storeId,
            @PathVariable Long orderId) {
        storeOrderService.cancelOrder(storeId, orderId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
