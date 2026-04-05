package com.dirtypay.domain.round.dto.request;

import com.dirtypay.domain.round.entity.RoundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 라운드 상태 변경 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class RoundStatusChangeRequest {

    @NotNull(message = "상태값은 필수입니다")
    private RoundStatus status;
}
