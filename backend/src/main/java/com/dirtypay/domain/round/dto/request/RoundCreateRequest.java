package com.dirtypay.domain.round.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 라운드 생성 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class RoundCreateRequest {

    @NotBlank(message = "라운드 제목은 필수입니다")
    private String title;

    private String place;

    private LocalDate roundDate;

    private int sortOrder;

    @NotNull(message = "가게 ID는 필수입니다")
    private Long storeId;
}
