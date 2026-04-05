package com.dirtypay.domain.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 지갑 송금 요청 DTO.
 *
 * <p>지갑 간 송금 시 클라이언트로부터 전달받는 요청 데이터를 담는다.
 * 송금 금액은 1원 이상이어야 하며, 수신자 이메일은 필수이다.
 * {@code idempotencyKey}가 null이면 서비스 계층에서 자동 생성된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class WalletTransferRequest {

    @NotBlank(message = "수신자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String receiverEmail;

    @NotNull(message = "송금 금액은 필수입니다")
    @DecimalMin(value = "1", message = "송금 금액은 1원 이상이어야 합니다")
    private BigDecimal amount;

    /**
     * 멱등성 키. null이면 서비스 계층에서 자동 생성된다.
     */
    private String idempotencyKey;

    /**
     * 거래 설명. nullable.
     */
    private String description;
}
