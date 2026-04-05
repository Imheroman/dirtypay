package com.dirtypay.domain.order.repository;

import com.dirtypay.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 주문 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 라운드 ID로 주문 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 주문 목록
     */
    List<Order> findByRoundId(Long roundId);

    /**
     * 라운드 ID와 그룹 ID로 주문 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @param groupId 그룹 ID
     * @return 주문 목록
     */
    List<Order> findByRoundIdAndGroupId(Long roundId, Long groupId);

    /**
     * 라운드 ID와 복수 그룹 ID로 주문 목록을 조회한다.
     *
     * @param roundId  라운드 ID
     * @param groupIds 그룹 ID 목록
     * @return 주문 목록
     */
    List<Order> findByRoundIdAndGroupIdIn(Long roundId, List<Long> groupIds);

    /**
     * 여러 라운드 ID에 속한 모든 주문을 한 번에 조회한다.
     *
     * <p>세션 정산 시 라운드별 반복 조회(N+1)를 단일 IN 쿼리로 대체한다.</p>
     *
     * @param roundIds 라운드 ID 목록
     * @return 주문 목록
     */
    List<Order> findByRoundIdIn(List<Long> roundIds);

    /**
     * 라운드에 주문이 존재하는지 확인한다.
     *
     * @param roundId 라운드 ID
     * @return 주문 존재 여부
     */
    boolean existsByRoundId(Long roundId);

    /**
     * 세션에 속한 모든 주문의 총 금액을 합산한다.
     *
     * <p>Round를 경유하여 세션의 주문 총액을 집계한다.
     * 주문이 없으면 0을 반환한다.</p>
     *
     * @param sessionId 세션 ID
     * @return 총 금액
     */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o " +
            "JOIN Round r ON o.roundId = r.id " +
            "WHERE r.sessionId = :sessionId")
    BigDecimal sumTotalPriceBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 라운드에 속한 주문의 총 금액을 합산한다.
     *
     * <p>주문이 없으면 0을 반환한다.</p>
     *
     * @param roundId 라운드 ID
     * @return 총 금액
     */
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o " +
            "WHERE o.roundId = :roundId")
    BigDecimal sumTotalPriceByRoundId(@Param("roundId") Long roundId);
}
