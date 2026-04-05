package com.dirtypay.domain.order.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.order.dto.request.OrderBatchCreateRequest;
import com.dirtypay.domain.order.dto.request.OrderCreateRequest;
import com.dirtypay.domain.order.dto.request.OrderUpdateRequest;
import com.dirtypay.domain.order.dto.response.OrderResponse;
import com.dirtypay.domain.order.service.OrderService;
import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.security.annotation.SessionAccess;
import com.dirtypay.global.security.annotation.SessionAccess.AccessLevel;
import com.dirtypay.global.security.annotation.SessionAccess.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 주문 컨트롤러.
 *
 * <p>주문 CRUD 및 조회 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 새로운 주문을 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param request       주문 생성 요청
     * @return 생성된 주문 응답
     */
    @Operation(summary = "주문 생성", description = "라운드에 새로운 주문을 생성합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PostMapping("/rounds/{roundId}/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @Valid @RequestBody OrderCreateRequest request) {

        OrderResponse response = this.orderService.createOrder(roundId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 주문을 일괄 생성한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param roundId       라운드 ID
     * @param request       주문 일괄 생성 요청
     * @return 생성된 주문 목록
     */
    @Operation(summary = "주문 일괄 생성", description = "라운드에 여러 주문을 일괄 생성합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @PostMapping("/rounds/{roundId}/orders/batch")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> createOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roundId,
            @Valid @RequestBody OrderBatchCreateRequest request) {

        List<OrderResponse> response = this.orderService.createOrders(roundId, request.getOrders());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 라운드의 주문 목록을 조회한다.
     *
     * @param roundId     라운드 ID
     * @param orgMemberId 멤버 ID (선택, 해당 멤버가 참여한 주문만 필터링)
     * @param groupId     그룹 ID (선택, 해당 그룹의 주문만 필터링)
     * @return 주문 목록
     */
    @Operation(summary = "주문 목록 조회", description = "라운드의 주문 목록을 조회합니다. orgMemberId, groupId로 필터링 가능합니다.")
    @GetMapping("/rounds/{roundId}/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @PathVariable Long roundId,
            @RequestParam(required = false) Long orgMemberId,
            @RequestParam(required = false) Long groupId) {

        List<OrderResponse> response = this.orderService.getOrdersByRound(roundId, orgMemberId, groupId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 주문 수량을 수정한다.
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param orderId       주문 ID
     * @param request       수정 요청
     * @return 수정된 주문 응답
     */
    @Operation(summary = "주문 수정", description = "주문의 수량을 수정합니다.")
    @SessionAccess(value = "orderId", type = ResourceType.ORDER, level = AccessLevel.MEMBER)
    @PutMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderUpdateRequest request) {

        OrderResponse response = this.orderService.updateOrder(orderId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 주문을 삭제한다. (Soft Delete)
     *
     * @param userPrincipal 인증된 사용자 정보
     * @param orderId       주문 ID
     * @return 빈 응답
     */
    @Operation(summary = "주문 삭제", description = "주문을 삭제합니다. (Soft Delete)")
    @SessionAccess(value = "orderId", type = ResourceType.ORDER, level = AccessLevel.MEMBER)
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long orderId) {

        this.orderService.deleteOrder(orderId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success());
    }
}
