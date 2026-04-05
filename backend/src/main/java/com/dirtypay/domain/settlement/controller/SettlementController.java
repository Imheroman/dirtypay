package com.dirtypay.domain.settlement.controller;

import com.dirtypay.domain.settlement.dto.request.SettlementPaymentRequest;
import com.dirtypay.domain.settlement.dto.response.MemberSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.NodeOrdersResponse;
import com.dirtypay.domain.settlement.dto.response.NodeSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.OrderSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.RoundSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.SessionSettlementResponse;
import com.dirtypay.domain.settlement.service.SettlementService;
import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정산 컨트롤러.
 *
 * <p>라운드별, 세션별, 멤버별, 그룹별 정산 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Settlement", description = "정산 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 라운드 정산을 조회한다.
     *
     * @param roundId  라운드 ID
     * @param strategy 나머지 분배 전략 (기본값: OWNER)
     * @return 라운드 정산 응답
     */
    @Operation(summary = "라운드 정산 조회", description = "라운드의 정산 결과를 조회합니다.")
    @GetMapping("/rounds/{roundId}/settlement")
    public ResponseEntity<ApiResponse<RoundSettlementResponse>> getRoundSettlement(
            @PathVariable Long roundId,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        RoundSettlementResponse response = this.settlementService
                .calculateRoundSettlement(roundId, strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션 정산을 조회한다.
     *
     * @param sessionId 세션 ID
     * @param strategy  나머지 분배 전략 (기본값: OWNER)
     * @return 세션 정산 응답
     */
    @Operation(summary = "세션 정산 조회", description = "세션의 전체 정산 결과를 조회합니다.")
    @GetMapping("/sessions/{sessionId}/settlement")
    public ResponseEntity<ApiResponse<SessionSettlementResponse>> getSessionSettlement(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        SessionSettlementResponse response = this.settlementService
                .calculateSessionSettlement(sessionId, strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 주문 중심 정산을 조회한다.
     *
     * @param sessionId 세션 ID
     * @param strategy  나머지 분배 전략 (기본값: OWNER)
     * @return 주문 중심 정산 응답
     */
    @Operation(summary = "주문별 정산 조회", description = "세션의 주문 중심 정산 결과를 카테고리별로 조회합니다.")
    @GetMapping("/sessions/{sessionId}/settlement/orders")
    public ResponseEntity<ApiResponse<OrderSettlementResponse>> getOrderSettlement(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        OrderSettlementResponse response = this.settlementService
                .calculateOrderSettlement(sessionId, strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 멤버별 정산 상세를 조회한다.
     *
     * @param sessionId   세션 ID
     * @param orgMemberId 멤버 ID
     * @param strategy    나머지 분배 전략 (기본값: OWNER)
     * @return 멤버별 정산 응답
     */
    @Operation(summary = "멤버별 정산 상세 조회", description = "특정 멤버의 정산 상세를 조회합니다.")
    @GetMapping("/sessions/{sessionId}/settlement/members/{orgMemberId}")
    public ResponseEntity<ApiResponse<MemberSettlementResponse>> getMemberSettlement(
            @PathVariable Long sessionId,
            @PathVariable Long orgMemberId,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        MemberSettlementResponse response = this.settlementService
                .calculateMemberSettlement(sessionId, orgMemberId, strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 그룹별 주문 내역을 조회한다.
     *
     * @param roundId 라운드 ID
     * @param groupId 그룹 ID
     * @return 그룹별 주문 내역 응답
     */
    @Operation(summary = "그룹별 주문 내역 조회",
            description = "특정 그룹의 주문 내역을 카테고리·메뉴별로 그룹핑하여 조회합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @GetMapping("/rounds/{roundId}/settlement/groups/{groupId}")
    public ResponseEntity<ApiResponse<NodeOrdersResponse>> getGroupOrders(
            @PathVariable Long roundId,
            @PathVariable Long groupId) {

        NodeOrdersResponse response = this.settlementService.getGroupOrders(roundId, groupId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 그룹별 정산 금액을 계산한다.
     *
     * @param roundId  라운드 ID
     * @param groupId  그룹 ID
     * @param strategy 나머지 분배 전략 (기본값: OWNER)
     * @return 그룹별 정산 응답
     */
    @Operation(summary = "그룹별 정산 금액 계산",
            description = "특정 그룹의 주문만으로 멤버별 정산 금액을 계산합니다.")
    @SessionAccess(value = "roundId", type = ResourceType.ROUND, level = AccessLevel.MEMBER)
    @GetMapping("/rounds/{roundId}/settlement/groups/{groupId}/amounts")
    public ResponseEntity<ApiResponse<NodeSettlementResponse>> calculateGroupSettlement(
            @PathVariable Long roundId,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        NodeSettlementResponse response = this.settlementService
                .calculateGroupSettlement(roundId, groupId, strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 멤버의 정산 완료 상태를 업데이트한다.
     *
     * @param sessionId   세션 ID
     * @param orgMemberId 멤버 ID
     * @param request     정산 완료 요청
     * @param strategy    나머지 분배 전략 (기본값: OWNER)
     * @return 멤버별 정산 응답
     */
    @Operation(summary = "정산 완료 표시", description = "멤버의 정산 납부 금액을 업데이트합니다.")
    @SessionAccess(value = "sessionId", level = AccessLevel.MEMBER)
    @PutMapping("/sessions/{sessionId}/settlement/members/{orgMemberId}")
    public ResponseEntity<ApiResponse<MemberSettlementResponse>> updateSettlementPayment(
            @PathVariable Long sessionId,
            @PathVariable Long orgMemberId,
            @Valid @RequestBody SettlementPaymentRequest request,
            @RequestParam(defaultValue = "OWNER") RemainderStrategyType strategy) {

        MemberSettlementResponse response = this.settlementService
                .updateSettlementPayment(sessionId, orgMemberId, request.getPaidAmount(), strategy);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
