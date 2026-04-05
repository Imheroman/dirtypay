package com.dirtypay.domain.group.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공유 메뉴 저장 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class SharedMenuSaveRequest {

    @NotNull(message = "메뉴 목록은 필수입니다")
    @Valid
    private List<SharedMenuItem> menus;

    /**
     * 공유 메뉴 항목.
     *
     * @author kim-young-woong
     * @since 1.0.0
     */
    @Getter
    @NoArgsConstructor
    public static class SharedMenuItem {

        @NotNull(message = "메뉴 ID는 필수입니다")
        private Long menuId;

        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        private int quantity;
    }
}
