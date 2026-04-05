package com.dirtypay.domain.order.repository;

import com.dirtypay.domain.order.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주문 상세 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    /**
     * 주문 ID로 주문 상세 목록을 조회한다.
     *
     * @param orderId 주문 ID
     * @return 주문 상세 목록
     */
    List<OrderDetail> findByOrderId(Long orderId);

    /**
     * 주문 ID 목록으로 주문 상세 목록을 일괄 조회한다.
     *
     * @param orderIds 주문 ID 목록
     * @return 주문 상세 목록
     */
    List<OrderDetail> findByOrderIdIn(List<Long> orderIds);
}
