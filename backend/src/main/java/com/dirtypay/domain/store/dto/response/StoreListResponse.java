package com.dirtypay.domain.store.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 매장 목록 응답 DTO.
 *
 * <p>페이지네이션 없이 전체 목록을 반환하는 일반 조회용 응답이다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class StoreListResponse {

    /** 매장 응답 목록. */
    private List<StoreResponse> stores;

    /** 총 매장 수. */
    private int totalCount;

    /**
     * 매장 응답 목록으로부터 목록 응답 DTO를 생성한다.
     *
     * @param stores 매장 응답 목록
     * @return 매장 목록 응답 DTO
     */
    public static StoreListResponse from(List<StoreResponse> stores) {
        return StoreListResponse.builder()
                .stores(stores)
                .totalCount(stores.size())
                .build();
    }
}
