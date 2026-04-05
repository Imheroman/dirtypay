package com.dirtypay.domain.store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 주문 생성 요청 DTO.
 *
 * <p>주문 번호(orderNumber)는 Service 계층에서 UUID로 생성한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreOrderCreateRequest {

    /**
     * 주문할 메뉴 ID. 필수.
     */
    @NotNull(message = "메뉴 ID는 필수입니다")
    private Long menuId;

    /**
     * 주문 수량. 필수, 1 이상.
     */
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    @Max(value = 9999, message = "수량은 최대 9999개입니다")
    private Integer quantity;

    /**
     * 주문자 회원 ID. 비회원 주문 시 null.
     */
    private Long memberId;

    /**
     * 주문자 이름. 선택, 최대 50자.
     */
    @Size(max = 50, message = "주문자 이름은 최대 50자입니다")
    private String customerName;

    /**
     * 주문자 전화번호. 선택, 최대 20자.
     */
    @Size(max = 20, message = "주문자 전화번호는 최대 20자입니다")
    private String customerPhone;
}
