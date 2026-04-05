package com.dirtypay.domain.wallet.dto.request;

import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 송금 요청 DTO.
 *
 * <p>세션 내 조직 멤버의 정산 송금 시 클라이언트로부터 전달받는 요청 데이터를 담는다.
 * 정산 금액은 서버에서 자동으로 계산되므로 요청 바디에는 나머지 분배 전략 유형만 포함된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class SettlementTransferRequest {

    /**
     * 나머지 분배 전략 유형. 정산 금액 계산 시 사용된다.
     */
    @NotNull(message = "나머지 분배 전략은 필수입니다")
    private RemainderStrategyType strategyType;
}
