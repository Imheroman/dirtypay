package com.dirtypay.domain.store.strategy;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.exception.BusinessException;

/**
 * 가게 등록 전략 인터페이스.
 *
 * <p>POS 연동({@link StoreType#POS_INTEGRATED}), 직접 등록({@link StoreType#DIRECT}),
 * 사용자 정의({@link StoreType#CUSTOM}) 세 가지 방식을 전략 패턴으로 지원한다.
 * 각 전략은 {@link #supports()}로 담당 타입을 선언하고,
 * {@link #validate(StoreType, String)}에서 타입별 유효성 검증, {@link #onRegister(Store)}에서
 * 등록 후처리를 수행한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface StoreRegistrationStrategy {

    /**
     * 이 전략이 지원하는 가게 등록 타입을 반환한다.
     *
     * @return 지원하는 {@link StoreType}
     */
    StoreType supports();

    /**
     * 가게 등록 시 필요한 유효성 검증을 수행한다.
     *
     * @param storeType         가게 타입
     * @param posIntegrationKey POS 연동 키 (타입에 따라 필수/선택)
     * @throws BusinessException 검증 실패 시
     */
    void validate(StoreType storeType, String posIntegrationKey);

    /**
     * 가게 등록 완료 후 처리 로직을 수행한다.
     *
     * @param store 등록된 가게 엔티티
     */
    void onRegister(Store store);
}
