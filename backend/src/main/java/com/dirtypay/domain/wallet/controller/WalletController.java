package com.dirtypay.domain.wallet.controller;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.wallet.dto.request.WalletChargeRequest;
import com.dirtypay.domain.wallet.dto.request.WalletTransferRequest;
import com.dirtypay.domain.wallet.dto.response.WalletResponse;
import com.dirtypay.domain.wallet.dto.response.WalletTransactionResponse;
import com.dirtypay.domain.wallet.service.WalletService;
import com.dirtypay.global.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 컨트롤러.
 *
 * <p>지갑 조회, 충전, 거래 이력 조회, 송금 API를 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Tag(name = "Wallet", description = "지갑 API")
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 현재 로그인한 사용자의 지갑 정보를 조회한다.
     *
     * @param userPrincipal 인증된 사용자 주체
     * @return 지갑 정보 응답
     */
    @Operation(summary = "내 지갑 조회", description = "현재 로그인한 사용자의 지갑 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        WalletResponse response = walletService.getWallet(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 사용자의 거래 이력을 페이징 조회한다.
     *
     * @param userPrincipal 인증된 사용자 주체
     * @param pageable      페이징 정보
     * @return 거래 이력 Page 응답
     */
    @Operation(summary = "거래 이력 조회", description = "현재 로그인한 사용자의 거래 이력을 페이징 조회합니다.")
    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {
        WalletResponse wallet = walletService.getWallet(userPrincipal.getId());
        Page<WalletTransactionResponse> response = walletService.getTransactions(wallet.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지갑에 금액을 충전한다.
     *
     * <p>일일 충전 한도는 3,000,000원이며, 초과 시 예외가 발생한다.</p>
     *
     * @param userPrincipal 인증된 사용자 주체
     * @param request       충전 요청 DTO
     * @return 충전 후 지갑 정보 응답
     */
    @Operation(summary = "지갑 충전", description = "지갑에 금액을 충전합니다. 일일 충전 한도: 3,000,000원")
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<WalletResponse>> charge(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WalletChargeRequest request) {
        WalletResponse response = walletService.charge(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 다른 사용자의 지갑으로 금액을 송금한다.
     *
     * @param userPrincipal 인증된 사용자 주체 (송신자)
     * @param request       송금 요청 DTO
     * @return TRANSFER_OUT 거래 응답
     */
    @Operation(summary = "지갑 송금", description = "다른 사용자의 지갑으로 금액을 송금합니다.")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletTransactionResponse>> transfer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WalletTransferRequest request) {
        WalletTransactionResponse response = walletService.transfer(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
