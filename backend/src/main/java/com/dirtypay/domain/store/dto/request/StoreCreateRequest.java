package com.dirtypay.domain.store.dto.request;

import com.dirtypay.domain.store.entity.StoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매장 생성 요청 DTO.
 *
 * <p>{@code storeType}이 {@link StoreType#POS_INTEGRATED}인 경우
 * {@code posIntegrationKey}가 필수이며, Service 계층에서 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class StoreCreateRequest {

    /**
     * 매장명. 필수, 최대 100자.
     */
    @NotBlank(message = "매장명은 필수입니다")
    @Size(max = 100, message = "매장명은 최대 100자입니다")
    private String name;

    /**
     * 사업자 등록번호. 최대 20자. 중복 검증은 Service에서 수행.
     */
    @Size(max = 20, message = "사업자 등록번호는 최대 20자입니다")
    private String businessNumber;

    /**
     * 매장 주소. 필수, 최대 255자.
     */
    @NotBlank(message = "매장 주소는 필수입니다")
    @Size(max = 255, message = "주소는 최대 255자입니다")
    private String address;

    /**
     * 매장 전화번호. 선택, 최대 20자.
     */
    @Size(max = 20, message = "전화번호는 최대 20자입니다")
    private String phone;

    /**
     * 매장 소개. 선택, 최대 1000자.
     */
    @Size(max = 1000, message = "매장 소개는 최대 1000자입니다")
    private String description;

    /**
     * 매장 유형. 필수.
     */
    @NotNull(message = "매장 유형은 필수입니다")
    private StoreType storeType;

    /**
     * POS 연동 키. {@link StoreType#POS_INTEGRATED}인 경우 필수. 최대 100자.
     */
    @Size(max = 100, message = "POS 연동 키는 최대 100자입니다")
    private String posIntegrationKey;
}
