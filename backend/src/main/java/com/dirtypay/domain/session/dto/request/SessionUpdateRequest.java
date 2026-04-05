package com.dirtypay.domain.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 세션 수정 요청 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class SessionUpdateRequest {

    @NotBlank(message = "세션 제목은 필수입니다")
    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;
}
