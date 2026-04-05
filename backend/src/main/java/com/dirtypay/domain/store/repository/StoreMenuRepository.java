package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.StoreMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 매장 메뉴 리포지토리.
 *
 * <p>{@code @SQLRestriction("deleted_date IS NULL")}에 의해 삭제된 엔티티가
 * 모든 쿼리에서 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface StoreMenuRepository extends JpaRepository<StoreMenu, Long> {

    /**
     * 매장 ID에 속한 전체 메뉴 목록을 조회한다.
     *
     * @param storeId 매장 ID
     * @return 메뉴 목록
     */
    List<StoreMenu> findAllByStoreId(Long storeId);

    /**
     * 메뉴 ID와 매장 ID로 메뉴를 조회한다.
     *
     * <p>메뉴가 해당 매장에 속하는지 함께 검증할 때 사용한다.</p>
     *
     * @param id      메뉴 ID
     * @param storeId 매장 ID
     * @return 메뉴 Optional
     */
    Optional<StoreMenu> findByIdAndStoreId(Long id, Long storeId);

    /**
     * 매장 ID와 판매 가능 여부로 메뉴 목록을 노출 순서 오름차순으로 조회한다.
     *
     * @param storeId   매장 ID
     * @param available 판매 가능 여부
     * @return 판매 가능 여부에 따른 메뉴 목록 (sortOrder 오름차순)
     */
    List<StoreMenu> findAllByStoreIdAndAvailableOrderBySortOrder(Long storeId, boolean available);
}
