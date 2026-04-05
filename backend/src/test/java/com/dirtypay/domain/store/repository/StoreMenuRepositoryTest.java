package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StoreMenuRepository} 통합 테스트.
 *
 * <p>매장 메뉴 조회, available 필터링, sortOrder 정렬, Soft Delete 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class StoreMenuRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreMenuRepository storeMenuRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private StoreMenu availableMenu1;
    private StoreMenu availableMenu2;
    private StoreMenu unavailableMenu;

    @BeforeEach
    void setUp() {
        storeMenuRepository.deleteAll();
        storeRepository.deleteAll();

        // 매장 생성
        store = storeRepository.save(Store.builder()
                .ownerId(1L)
                .name("테스트 매장")
                .businessNumber("111-11-11111")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .status(StoreStatus.ACTIVE)
                .build());

        // 판매 가능 메뉴 (sortOrder: 2)
        availableMenu1 = storeMenuRepository.save(StoreMenu.builder()
                .storeId(store.getId())
                .name("삼겹살")
                .price(new BigDecimal("15000"))
                .available(true)
                .sortOrder(2)
                .build());

        // 판매 가능 메뉴 (sortOrder: 1) — 정렬 상 먼저 노출
        availableMenu2 = storeMenuRepository.save(StoreMenu.builder()
                .storeId(store.getId())
                .name("소주")
                .price(new BigDecimal("5000"))
                .available(true)
                .sortOrder(1)
                .build());

        // 판매 불가 메뉴
        unavailableMenu = storeMenuRepository.save(StoreMenu.builder()
                .storeId(store.getId())
                .name("품절 메뉴")
                .price(new BigDecimal("10000"))
                .available(false)
                .sortOrder(3)
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("storeId로 메뉴 조회 테스트")
    class FindAllByStoreIdTest {

        @Test
        @DisplayName("findAllByStoreId: 매장의 모든 메뉴(판매 가능/불가 포함)를 반환한다")
        void StoreMenuRepository_findByStoreId_returnsSortedMenus() {
            // when
            List<StoreMenu> menus = storeMenuRepository.findAllByStoreId(store.getId());

            // then
            assertThat(menus).hasSize(3);
            assertThat(menus).extracting(StoreMenu::getName)
                    .containsExactlyInAnyOrder("삼겹살", "소주", "품절 메뉴");
        }

        @Test
        @DisplayName("findAllByStoreId: 메뉴 없는 매장 조회 시 빈 리스트를 반환한다")
        void StoreMenuRepository_findByStoreId_emptyStore_returnsEmpty() {
            // given — 다른 매장 (메뉴 없음)
            Store emptyStore = storeRepository.save(Store.builder()
                    .ownerId(2L)
                    .name("빈 매장")
                    .businessNumber("999-99-99999")
                    .address("제주시 노형동")
                    .storeType(StoreType.DIRECT)
                    .status(StoreStatus.ACTIVE)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // when
            List<StoreMenu> menus = storeMenuRepository.findAllByStoreId(emptyStore.getId());

            // then
            assertThat(menus).isEmpty();
        }
    }

    @Nested
    @DisplayName("available 필터 조회 테스트")
    class FindByAvailableTest {

        @Test
        @DisplayName("findAllByStoreIdAndAvailableOrderBySortOrder: available=true인 메뉴만 sortOrder 오름차순으로 반환한다")
        void StoreMenuRepository_findByStoreIdAndAvailable_returnsAvailableOnly() {
            // when
            List<StoreMenu> availableMenus = storeMenuRepository
                    .findAllByStoreIdAndAvailableOrderBySortOrder(store.getId(), true);

            // then
            assertThat(availableMenus).hasSize(2);
            assertThat(availableMenus).extracting(StoreMenu::getName)
                    .doesNotContain("품절 메뉴");
            // sortOrder 오름차순 검증: 소주(1) -> 삼겹살(2)
            assertThat(availableMenus.get(0).getName()).isEqualTo("소주");
            assertThat(availableMenus.get(1).getName()).isEqualTo("삼겹살");
        }
    }

    @Nested
    @DisplayName("menuId + storeId 복합 조회 테스트")
    class FindByIdAndStoreIdTest {

        @Test
        @DisplayName("findByIdAndStoreId: 올바른 menuId + storeId 조합으로 메뉴를 조회한다")
        void StoreMenuRepository_findByIdAndStoreId_returnsMenu() {
            // when
            Optional<StoreMenu> result = storeMenuRepository
                    .findByIdAndStoreId(availableMenu1.getId(), store.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("삼겹살");
        }

        @Test
        @DisplayName("findByIdAndStoreId: storeId가 불일치하면 빈 Optional을 반환한다")
        void StoreMenuRepository_findByIdAndStoreId_throwsWhenStoreIdMismatch() {
            // when
            Optional<StoreMenu> result = storeMenuRepository
                    .findByIdAndStoreId(availableMenu1.getId(), 9999L);  // 다른 storeId

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("소프트 삭제 필터링 테스트")
    class SoftDeleteTest {

        @Test
        @DisplayName("@SQLRestriction: Soft Delete된 메뉴는 조회에서 자동으로 제외된다")
        void StoreMenuRepository_sqlRestriction_excludesDeleted() {
            // given — 삭제 전 메뉴 수 확인
            List<StoreMenu> beforeDelete = storeMenuRepository.findAllByStoreId(store.getId());
            int countBefore = beforeDelete.size();

            StoreMenu menuToDelete = storeMenuRepository.findById(availableMenu1.getId()).get();
            menuToDelete.delete();
            storeMenuRepository.save(menuToDelete);
            entityManager.flush();
            entityManager.clear();

            // when
            List<StoreMenu> afterDelete = storeMenuRepository.findAllByStoreId(store.getId());

            // then
            assertThat(afterDelete).hasSize(countBefore - 1);
            assertThat(afterDelete).extracting(StoreMenu::getName).doesNotContain("삼겹살");
        }
    }
}
