package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 매장 주문 리포지토리.
 *
 * <p>{@code @SQLRestriction("deleted_date IS NULL")}에 의해 삭제된 엔티티가
 * 모든 쿼리에서 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {

    /**
     * 매장 ID에 속한 주문을 페이지 단위로 생성일시 내림차순으로 조회한다.
     *
     * @param storeId  매장 ID
     * @param pageable 페이지 요청 정보
     * @return 주문 페이지 (최신순)
     */
    Page<StoreOrder> findAllByStoreIdOrderByCreatedDateDesc(Long storeId, Pageable pageable);

    /**
     * 매장 ID와 주문 상태로 주문을 페이지 단위로 생성일시 내림차순으로 조회한다.
     *
     * @param storeId  매장 ID
     * @param status   조회할 주문 상태
     * @param pageable 페이지 요청 정보
     * @return 해당 상태의 주문 페이지 (최신순)
     */
    Page<StoreOrder> findAllByStoreIdAndStatusOrderByCreatedDateDesc(Long storeId, StoreOrderStatus status, Pageable pageable);

    /**
     * 주문 ID와 매장 ID로 주문을 조회한다.
     *
     * <p>주문이 해당 매장에 속하는지 함께 검증할 때 사용한다.</p>
     *
     * @param id      주문 ID
     * @param storeId 매장 ID
     * @return 주문 Optional
     */
    Optional<StoreOrder> findByIdAndStoreId(Long id, Long storeId);

    /**
     * 기간 내 매장의 총 주문 건수를 조회한다.
     *
     * <p>지정된 상태의 주문만 집계한다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작 일시 (포함)
     * @param endDate   조회 종료 일시 (포함)
     * @param statuses  집계 대상 주문 상태 목록
     * @return 총 주문 건수
     */
    @Query("SELECT COUNT(o) FROM StoreOrder o " +
            "WHERE o.storeId = :storeId " +
            "AND o.createdDate >= :startDate " +
            "AND o.createdDate <= :endDate " +
            "AND o.status IN :statuses")
    long countByStoreIdAndPeriod(@Param("storeId") Long storeId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("statuses") List<StoreOrderStatus> statuses);

    /**
     * 기간 내 매장의 총 매출액을 조회한다.
     *
     * <p>지정된 상태의 주문만 집계한다.
     * 주문이 없으면 0을 반환한다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작 일시 (포함)
     * @param endDate   조회 종료 일시 (포함)
     * @param statuses  집계 대상 주문 상태 목록
     * @return 총 매출액
     */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM StoreOrder o " +
            "WHERE o.storeId = :storeId " +
            "AND o.createdDate >= :startDate " +
            "AND o.createdDate <= :endDate " +
            "AND o.status IN :statuses")
    BigDecimal sumTotalPriceByStoreIdAndPeriod(@Param("storeId") Long storeId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("statuses") List<StoreOrderStatus> statuses);

    /**
     * 기간 내 매장의 메뉴별 주문 건수와 매출액을 주문 건수 내림차순으로 조회한다.
     *
     * <p>주의: StoreMenu의 {@code @SQLRestriction}에 의해 soft-deleted 메뉴는
     * JOIN에서 제외된다. 삭제된 메뉴의 과거 주문 통계가 필요하면 Native Query로 변경해야 한다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작 일시 (포함)
     * @param endDate   조회 종료 일시 (포함)
     * @return 메뉴별 집계 결과 목록 (orderCount 내림차순)
     */
    @Query("SELECT o.menuId AS menuId, m.name AS menuName, COUNT(o) AS orderCount, " +
            "COALESCE(SUM(o.totalPrice), 0) AS revenue " +
            "FROM StoreOrder o " +
            "JOIN StoreMenu m ON m.id = o.menuId " +
            "WHERE o.storeId = :storeId " +
            "AND o.createdDate >= :startDate " +
            "AND o.createdDate <= :endDate " +
            "GROUP BY o.menuId, m.name " +
            "ORDER BY COUNT(o) DESC")
    List<PopularMenuProjection> findPopularMenusByPeriod(@Param("storeId") Long storeId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
}
