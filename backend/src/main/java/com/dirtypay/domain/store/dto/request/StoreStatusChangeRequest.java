package com.dirtypay.domain.store.dto.request;

import com.dirtypay.domain.store.entity.StoreStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 상태 변경 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreStatusChangeRequest {

    /**
     * 변경할 매장 상태. 필수.
     */
    @NotNull(message = "매장 상태는 필수입니다")
    private StoreStatus status;
}
