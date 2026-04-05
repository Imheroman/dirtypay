package com.dirtypay.global.security;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.store.repository.StoreOrderRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import com.dirtypay.global.security.annotation.StoreOwner.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리소스 ID와 타입으로부터 매장 소유자 ID를 조회하는 Resolver.
 *
 * <p>리소스 타입에 따라 엔티티 체인을 따라가며 최종 매장 소유자({@code ownerId})를 반환한다.
 * {@link StoreOwnerAspect}에서 인증/인가 판단과 분리된 엔티티 조회 책임을 담당한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class StoreOwnerResolver {

    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreOrderRepository storeOrderRepository;

    /**
     * 리소스 ID와 타입으로부터 매장 소유자 ID를 조회한다.
     *
     * <p>STORE_MENU, STORE_ORDER 타입은 해당 리소스의 {@code storeId}를 경유하여
     * 최종 {@link Store#getOwnerId()}를 반환한다.</p>
     *
     * @param resourceId   리소스 ID
     * @param resourceType 리소스 타입 ({@link ResourceType})
     * @return 매장 소유자 ID
     * @throws EntityNotFoundException 체인 조회 중 엔티티를 찾을 수 없는 경우
     */
    public Long resolveOwnerId(ResourceType resourceType, Long resourceId) {
        return switch (resourceType) {
            case STORE -> this.findStore(resourceId).getOwnerId();
            case STORE_MENU -> {
                StoreMenu menu = this.findMenu(resourceId);
                yield this.findStore(menu.getStoreId()).getOwnerId();
            }
            case STORE_ORDER -> {
                StoreOrder order = this.findOrder(resourceId);
                yield this.findStore(order.getStoreId()).getOwnerId();
            }
        };
    }

    private Store findStore(Long storeId) {
        return this.storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_NOT_FOUND));
    }

    private StoreMenu findMenu(Long menuId) {
        return this.storeMenuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));
    }

    private StoreOrder findOrder(Long orderId) {
        return this.storeOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_ORDER_NOT_FOUND));
    }
}
