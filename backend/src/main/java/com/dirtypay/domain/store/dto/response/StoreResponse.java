package com.dirtypay.domain.store.dto.response;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 매장 상세 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreResponse {

    /** 매장 ID. */
    private Long id;

    /** 소유자 회원 ID. */
    private Long ownerId;

    /** 매장명. */
    private String name;

    /** 사업자 등록번호. */
    private String businessNumber;

    /** 매장 주소. */
    private String address;

    /** 매장 전화번호. */
    private String phone;

    /** 매장 소개. */
    private String description;

    /** 매장 유형. */
    private StoreType storeType;

    /** 매장 운영 상태. */
    private StoreStatus status;

    /** 생성 일시. */
    private LocalDateTime createdDate;

    /** 수정 일시. */
    private LocalDateTime updatedDate;

    /**
     * Store 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param store 매장 엔티티
     * @return 매장 응답 DTO
     */
    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .ownerId(store.getOwnerId())
                .name(store.getName())
                .businessNumber(store.getBusinessNumber())
                .address(store.getAddress())
                .phone(store.getPhone())
                .description(store.getDescription())
                .storeType(store.getStoreType())
                .status(store.getStatus())
                .createdDate(store.getCreatedDate())
                .updatedDate(store.getUpdatedDate())
                .build();
    }
}
