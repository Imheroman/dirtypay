package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StoreOrderRepository} 통합 테스트.
 *
 * <p>주문 조회, 상태별 필터링, COUNT/SUM 집계 쿼리, 인기 메뉴 GROUP BY, Soft Delete 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class StoreOrderRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreMenuRepository storeMenuRepository;

    @Autowired
    private StoreOrderRepository storeOrderRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private StoreMenu menuA;
    private StoreMenu menuB;
    private StoreOrder pendingOrder;
    private StoreOrder confirmedOrder;
    private StoreOrder completedOrder;

    @BeforeEach
    void setUp() {
        storeOrderRepository.deleteAll();
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

        // 메뉴 생성
        menuA = storeMenuRepository.save(StoreMenu.builder()
                .storeId(store.getId())
                .name("삼겹살")
                .price(new BigDecimal("15000"))
                .available(true)
                .sortOrder(1)
                .build());

        menuB = storeMenuRepository.save(StoreMenu.builder()
                .storeId(store.getId())
                .name("소주")
                .price(new BigDecimal("5000"))
                .available(true)
                .sortOrder(2)
                .build());

        // 주문 생성
        pendingOrder = storeOrderRepository.save(StoreOrder.builder()
                .storeId(store.getId())
                .menuId(menuA.getId())
                .quantity(2)
                .totalPrice(new BigDecimal("30000"))
                .unitPrice(new BigDecimal("15000"))
                .menuName("삼겹살")
                .orderNumber(UUID.randomUUID().toString())
                .build());

        confirmedOrder = storeOrderRepository.save(StoreOrder.builder()
                .storeId(store.getId())
                .menuId(menuA.getId())
                .quantity(1)
                .totalPrice(new BigDecimal("15000"))
                .unitPrice(new BigDecimal("15000"))
                .menuName("삼겹살")
                .orderNumber(UUID.randomUUID().toString())
                .build());
        confirmedOrder.confirm();
        storeOrderRepository.save(confirmedOrder);

        completedOrder = storeOrderRepository.save(StoreOrder.builder()
                .storeId(store.getId())
                .menuId(menuB.getId())
                .quantity(3)
                .totalPrice(new BigDecimal("15000"))
                .unitPrice(new BigDecimal("5000"))
                .menuName("소주")
                .orderNumber(UUID.randomUUID().toString())
                .build());
        completedOrder.confirm();
        completedOrder.complete();
        storeOrderRepository.save(completedOrder);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("storeId로 주문 조회 테스트")
    class FindByStoreIdTest {

        @Test
        @DisplayName("findAllByStoreIdOrderByCreatedDateDesc: 매장의 모든 주문을 최신순으로 반환한다")
        void StoreOrderRepository_findByStoreId_returnsOrders() {
            // when
            List<StoreOrder> orders = storeOrderRepository
                    .findAllByStoreIdOrderByCreatedDateDesc(store.getId(), Pageable.unpaged())
                    .getContent();

            // then
            assertThat(orders).hasSize(3);
        }

        @Test
        @DisplayName("findByIdAndStoreId: 올바른 orderId + storeId 조합으로 주문을 조회한다")
        void StoreOrderRepository_findByIdAndStoreId_success() {
            // when
            Optional<StoreOrder> result = storeOrderRepository
                    .findByIdAndStoreId(pendingOrder.getId(), store.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(StoreOrderStatus.PENDING);
        }

        @Test
        @DisplayName("findByIdAndStoreId: storeId가 불일치하면 빈 Optional을 반환한다")
        void StoreOrderRepository_findByIdAndStoreId_throwsWhenNotFound() {
            // when
            Optional<StoreOrder> result = storeOrderRepository
                    .findByIdAndStoreId(pendingOrder.getId(), 9999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("상태별 주문 조회 테스트")
    class FindByStatusTest {

        @Test
        @DisplayName("findAllByStoreIdAndStatusOrderByCreatedDateDesc: PENDING 상태 주문만 필터링한다")
        void StoreOrderRepository_findByStoreIdAndStatus_filtersCorrectly() {
            // when
            List<StoreOrder> pendingOrders = storeOrderRepository
                    .findAllByStoreIdAndStatusOrderByCreatedDateDesc(
                            store.getId(), StoreOrderStatus.PENDING, Pageable.unpaged())
                    .getContent();

            List<StoreOrder> completedOrders = storeOrderRepository
                    .findAllByStoreIdAndStatusOrderByCreatedDateDesc(
                            store.getId(), StoreOrderStatus.COMPLETED, Pageable.unpaged())
                    .getContent();

            // then
            assertThat(pendingOrders).hasSize(1);
            assertThat(pendingOrders.get(0).getStatus()).isEqualTo(StoreOrderStatus.PENDING);

            assertThat(completedOrders).hasSize(1);
            assertThat(completedOrders.get(0).getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("집계 쿼리 테스트")
    class AggregationTest {

        @Test
        @DisplayName("countByStoreIdAndPeriod: 기간 내 매장 총 주문 건수를 반환한다")
        void StoreOrderRepository_countByStoreId_returnsCorrectCount() {
            // given
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<StoreOrderStatus> allStatuses = List.of(
                    StoreOrderStatus.PENDING, StoreOrderStatus.CONFIRMED,
                    StoreOrderStatus.COMPLETED, StoreOrderStatus.CANCELLED);

            // when
            long count = storeOrderRepository.countByStoreIdAndPeriod(store.getId(), start, end, allStatuses);

            // then
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("sumTotalPriceByStoreIdAndPeriod: 기간 내 매장 총 매출액을 반환한다")
        void StoreOrderRepository_sumTotalPriceByStoreId_returnsTotal() {
            // given — 30000 + 15000 + 15000 = 60000
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<StoreOrderStatus> allStatuses = List.of(
                    StoreOrderStatus.PENDING, StoreOrderStatus.CONFIRMED,
                    StoreOrderStatus.COMPLETED, StoreOrderStatus.CANCELLED);

            // when
            BigDecimal total = storeOrderRepository.sumTotalPriceByStoreIdAndPeriod(store.getId(), start, end, allStatuses);

            // then
            assertThat(total).isEqualByComparingTo(new BigDecimal("60000"));
        }

        @Test
        @DisplayName("countByStoreIdAndPeriod: 해당 기간에 주문이 없으면 0을 반환한다")
        void StoreOrderRepository_countByStoreId_returnsZeroWhenNoOrders() {
            // given — 미래 기간
            LocalDateTime start = LocalDateTime.now().plusDays(10);
            LocalDateTime end = LocalDateTime.now().plusDays(20);
            List<StoreOrderStatus> allStatuses = List.of(
                    StoreOrderStatus.PENDING, StoreOrderStatus.CONFIRMED,
                    StoreOrderStatus.COMPLETED, StoreOrderStatus.CANCELLED);

            // when
            long count = storeOrderRepository.countByStoreIdAndPeriod(store.getId(), start, end, allStatuses);

            // then
            assertThat(count).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("인기 메뉴 조회 테스트")
    class PopularMenuTest {

        @Test
        @DisplayName("findPopularMenusByPeriod: 메뉴별 주문 건수를 내림차순으로 집계하여 반환한다")
        void StoreOrderRepository_countByMenuIdGroupBy_returnsPopularMenus() {
            // given — menuA: 2건, menuB: 1건
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);

            // when
            List<PopularMenuProjection> results = storeOrderRepository.findPopularMenusByPeriod(store.getId(), start, end);

            // then
            assertThat(results).hasSize(2);
            // 첫 번째가 menuA (주문 2건, 내림차순)
            PopularMenuProjection firstRow = results.get(0);
            assertThat(firstRow.getMenuId()).isEqualTo(menuA.getId());
            assertThat(firstRow.getMenuName()).isEqualTo("삼겹살");
            assertThat(firstRow.getOrderCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("소프트 삭제 필터링 테스트")
    class SoftDeleteTest {

        @Test
        @DisplayName("@SQLRestriction: Soft Delete된 주문은 통계 쿼리에서 제외된다")
        void StoreOrderRepository_sqlRestriction_excludesDeletedFromStats() {
            // given — pendingOrder 소프트 삭제
            StoreOrder orderToDelete = storeOrderRepository.findById(pendingOrder.getId()).get();
            orderToDelete.delete();
            storeOrderRepository.save(orderToDelete);
            entityManager.flush();
            entityManager.clear();

            LocalDateTime start = LocalDateTime.now().minusDays(1);
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            List<StoreOrderStatus> allStatuses = List.of(
                    StoreOrderStatus.PENDING, StoreOrderStatus.CONFIRMED,
                    StoreOrderStatus.COMPLETED, StoreOrderStatus.CANCELLED);

            // when
            long count = storeOrderRepository.countByStoreIdAndPeriod(store.getId(), start, end, allStatuses);
            BigDecimal total = storeOrderRepository.sumTotalPriceByStoreIdAndPeriod(store.getId(), start, end, allStatuses);

            // then — 삭제된 주문(30000) 제외: 15000 + 15000 = 30000
            assertThat(count).isEqualTo(2L);
            assertThat(total).isEqualByComparingTo(new BigDecimal("30000"));
        }
    }
}
