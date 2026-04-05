package com.dirtypay.domain.store.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 매장 메뉴 수정 요청 DTO.
 *
 * <p>모든 필드가 선택 항목이며, null이 아닌 필드만 업데이트한다.
 * 실제 적용 로직은 Service 계층에서 수행한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreMenuUpdateRequest {

    /**
     * 변경할 메뉴명. 선택, 최대 100자.
     */
    @Size(max = 100, message = "메뉴명은 최대 100자입니다")
    private String name;

    /**
     * 변경할 메뉴 설명. 선택, 최대 500자.
     */
    @Size(max = 500, message = "메뉴 설명은 최대 500자입니다")
    private String description;

    /**
     * 변경할 가격. 선택, 0 초과.
     */
    @DecimalMin(value = "0.01", message = "메뉴 가격은 0보다 커야 합니다")
    private BigDecimal price;

    /**
     * 변경할 카테고리. 선택, 최대 50자.
     */
    @Size(max = 50, message = "카테고리는 최대 50자입니다")
    private String category;

    /**
     * 변경할 이미지 URL. 선택, 최대 500자.
     */
    @Size(max = 500, message = "이미지 URL은 최대 500자입니다")
    private String imageUrl;

    /**
     * 변경할 노출 순서. 선택, 0 이상.
     */
    @Min(value = 0, message = "노출 순서는 0 이상이어야 합니다")
    private Integer sortOrder;
}
