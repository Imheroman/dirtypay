package com.dirtypay.domain.store.strategy;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * POS 연동 가게 등록 전략.
 *
 * <p>{@link StoreType#POS_INTEGRATED} 타입 매장에 대한 등록 전략이다.
 * POS 연동 키({@code posIntegrationKey})가 필수이며, null이거나 비어 있으면
 * {@link BusinessException}({@link ErrorCode#STORE_POS_KEY_REQUIRED})을 던진다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Component
public class PosIntegrationStrategy implements StoreRegistrationStrategy {

    /**
     * {@inheritDoc}
     *
     * @return {@link StoreType#POS_INTEGRATED}
     */
    @Override
    public StoreType supports() {
        return StoreType.POS_INTEGRATED;
    }

    /**
     * POS 연동 등록 유효성을 검증한다.
     *
     * <p>{@code posIntegrationKey}가 null이거나 비어 있으면
     * {@link BusinessException}({@link ErrorCode#STORE_POS_KEY_REQUIRED})을 던진다.</p>
     *
     * @param storeType         가게 타입 (POS_INTEGRATED)
     * @param posIntegrationKey POS 연동 키 (필수)
     * @throws BusinessException POS 연동 키가 없는 경우
     */
    @Override
    public void validate(StoreType storeType, String posIntegrationKey) {
        if (posIntegrationKey == null || posIntegrationKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORE_POS_KEY_REQUIRED);
        }
        log.debug("POS 연동 전략 검증 통과: storeType={}", storeType);
    }

    /**
     * POS 연동 등록 완료 후 처리를 수행한다.
     *
     * @param store 등록된 가게 엔티티
     */
    @Override
    public void onRegister(Store store) {
        log.info("POS 연동 등록 완료: storeId={}", store.getId());
    }
}
