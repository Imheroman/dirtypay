package com.dirtypay.domain.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO.
 *
 * <p>memberIds가 null이면 해당 그룹 + 하위 그룹의 전체 멤버가 자동 포함된다.
 * memberIds가 제공되면 허용 범위(그룹 + 하위 그룹 멤버) 내인지 검증 후 해당 멤버만 포함된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "주문 그룹 ID는 필수입니다")
    private Long groupId;

    @NotNull(message = "메뉴 ID는 필수입니다")
    private Long menuId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    @Max(value = 50, message = "수량은 최대 50개입니다")
    private int quantity;

    private List<Long> memberIds;
}
