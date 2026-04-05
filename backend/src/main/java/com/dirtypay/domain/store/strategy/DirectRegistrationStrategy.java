package com.dirtypay.domain.store.strategy;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 직접 등록 가게 전략.
 *
 * <p>POS 연동 없이 직접 운영하는 매장({@link StoreType#DIRECT})에 대한
 * 등록 전략이다. {@code posIntegrationKey}는 검증하지 않으며, 기본 등록 흐름을 따른다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Component
public class DirectRegistrationStrategy implements StoreRegistrationStrategy {

    /**
     * {@inheritDoc}
     *
     * @return {@link StoreType#DIRECT}
     */
    @Override
    public StoreType supports() {
        return StoreType.DIRECT;
    }

    /**
     * 직접 등록 유형 검증을 수행한다.
     *
     * <p>{@code posIntegrationKey}는 검증하지 않으며, 별도의 비즈니스 규칙 검증만 수행한다.</p>
     *
     * @param storeType         가게 타입 (DIRECT)
     * @param posIntegrationKey 무시됨
     */
    @Override
    public void validate(StoreType storeType, String posIntegrationKey) {
        log.debug("직접 등록 전략 검증: storeType={}", storeType);
    }

    /**
     * 직접 등록 완료 후 처리를 수행한다.
     *
     * @param store 등록된 가게 엔티티
     */
    @Override
    public void onRegister(Store store) {
        log.info("직접 등록 완료: storeId={}, name={}", store.getId(), store.getName());
    }
}
