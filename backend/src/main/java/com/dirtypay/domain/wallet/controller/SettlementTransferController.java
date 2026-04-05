package com.dirtypay.domain.wallet.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.wallet.dto.request.SettlementTransferRequest;
import com.dirtypay.domain.wallet.dto.response.SettlementTransferResponse;
import com.dirtypay.domain.wallet.service.SettlementTransferService;
import com.dirtypay.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 정산 송금 컨트롤러.
 *
 * <p>세션 내 정산 송금 실행, 현황 조회, 취소 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Settlement Transfer", description = "정산 송금 API")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementTransferController {

    private final SettlementTransferService settlementTransferService;

    /**
     * 정산 금액을 총무에게 송금한다.
     *
     * <p>참여자가 본인의 정산 금액을 세션 총무 지갑으로 송금한다.
     * 정산 금액은 서버에서 자동 계산되며, 요청자는 나머지 분배 전략과 조직 멤버 ID를 제공한다.</p>
     *
     * @param sessionId     세션 ID
     * @param orgMemberId   조직 멤버 ID (송금자)
     * @param request       정산 송금 요청 DTO (나머지 분배 전략 포함)
     * @param userPrincipal 인증된 사용자 주체
     * @return 생성된 정산 송금 응답 (HTTP 201 Created)
     */
    @Operation(summary = "정산 송금 실행", description = "정산 금액을 기반으로 총무에게 송금합니다.")
    @PostMapping("/{sessionId}/transfers")
    public ResponseEntity<ApiResponse<SettlementTransferResponse>> createTransfer(
            @PathVariable Long sessionId,
            @RequestParam Long orgMemberId,
            @Valid @RequestBody SettlementTransferRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        SettlementTransferResponse response = settlementTransferService
                .createSettlementTransfer(sessionId, orgMemberId, request.getStrategyType());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 세션의 정산 송금 현황을 조회한다.
     *
     * <p>해당 세션에 대한 모든 정산 송금 내역 목록을 반환한다.</p>
     *
     * @param sessionId 세션 ID
     * @return 정산 송금 응답 목록
     */
    @Operation(summary = "정산 송금 현황 조회", description = "세션의 정산 송금 내역을 조회합니다.")
    @GetMapping("/{sessionId}/transfers")
    public ResponseEntity<ApiResponse<List<SettlementTransferResponse>>> getTransfers(
            @PathVariable Long sessionId) {

        List<SettlementTransferResponse> response = settlementTransferService
                .getSettlementTransfers(sessionId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    /**
     * 대기 중인 정산 송금을 취소하고 환불한다.
     *
     * <p>{@code PENDING} 상태의 정산 송금만 취소할 수 있다.
     * 취소 시 수신자 지갑에서 송신자 지갑으로 역방향 이체가 실행된다.</p>
     *
     * @param transferId    취소할 정산 송금 ID
     * @param userPrincipal 인증된 사용자 주체
     * @return 취소된 정산 송금 응답
     */
    @Operation(summary = "정산 송금 취소", description = "대기 중인 정산 송금을 취소하고 환불합니다.")
    @PostMapping("/transfers/{transferId}/cancel")
    public ResponseEntity<ApiResponse<SettlementTransferResponse>> cancelTransfer(
            @PathVariable Long transferId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        SettlementTransferResponse response = settlementTransferService
                .cancelSettlementTransfer(transferId, userPrincipal.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
