package com.dirtypay.domain.store.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 수정 요청 DTO.
 *
 * <p>모든 필드가 선택 항목이며, null이 아닌 필드만 업데이트한다.
 * 실제 적용 로직은 Service 계층에서 수행한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreUpdateRequest {

    /**
     * 변경할 매장명. 선택, 최대 100자.
     */
    @Size(max = 100, message = "매장명은 최대 100자입니다")
    private String name;

    /**
     * 변경할 주소. 선택, 최대 255자.
     */
    @Size(max = 255, message = "주소는 최대 255자입니다")
    private String address;

    /**
     * 변경할 전화번호. 선택, 최대 20자.
     */
    @Size(max = 20, message = "전화번호는 최대 20자입니다")
    private String phone;

    /**
     * 변경할 소개. 선택, 최대 1000자.
     */
    @Size(max = 1000, message = "매장 소개는 최대 1000자입니다")
    private String description;
}
