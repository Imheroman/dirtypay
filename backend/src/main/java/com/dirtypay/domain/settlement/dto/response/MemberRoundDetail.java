package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 멤버별 라운드 상세 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberRoundDetail {

    private Long roundId;
    private BigDecimal amount;
    private List<MemberOrderDetail> orders;
}
