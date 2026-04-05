package com.dirtypay.domain.order.repository;

import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderDetailRepository} 통합 테스트.
 *
 * <p>주문 ID 기반 상세 조회, 여러 주문 ID 일괄 조회,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class OrderDetailRepositoryTest {

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private EntityManager entityManager;

    // 주문 ID 상수 (Order 엔티티 없이 FK 값만 사용)
    private static final Long ORDER_1_ID = 1L;
    private static final Long ORDER_2_ID = 2L;
    private static final Long ORDER_3_ID = 3L;

    // 테스트 픽스처
    private OrderDetail detail1Order1;
    private OrderDetail detail2Order1;
    private OrderDetail detail1Order2;
    private OrderDetail detail1Order3;
    private OrderDetail deletedDetailOrder1;

    @BeforeEach
    void setUp() {
        orderDetailRepository.deleteAll();

        // Order 1에 속한 활성 상세 2개 (멤버 2명이 참여)
        detail1Order1 = orderDetailRepository.save(OrderDetail.builder()
                .orderId(ORDER_1_ID)
                .orgMemberId(10L)
                .shareRatio(1)
                .build());

        detail2Order1 = orderDetailRepository.save(OrderDetail.builder()
                .orderId(ORDER_1_ID)
                .orgMemberId(20L)
                .shareRatio(2)
                .build());

        // Order 2에 속한 활성 상세 1개
        detail1Order2 = orderDetailRepository.save(OrderDetail.builder()
                .orderId(ORDER_2_ID)
                .orgMemberId(10L)
                .shareRatio(1)
                .build());

        // Order 3에 속한 활성 상세 1개
        detail1Order3 = orderDetailRepository.save(OrderDetail.builder()
                .orderId(ORDER_3_ID)
                .orgMemberId(30L)
                .shareRatio(1)
                .build());

        // Order 1에 속하지만 소프트 삭제된 상세
        deletedDetailOrder1 = orderDetailRepository.save(OrderDetail.builder()
                .orderId(ORDER_1_ID)
                .orgMemberId(99L)
                .shareRatio(1)
                .build());
        deletedDetailOrder1.delete();
        orderDetailRepository.save(deletedDetailOrder1);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByOrderId 테스트")
    class FindByOrderIdTest {

        @Test
        @DisplayName("Order 1의 상세 목록을 조회하면 해당 주문의 상세만 반환된다")
        void findByOrderId_returnsDetailsForOrder1() {
            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderId(ORDER_1_ID);

            // then
            assertThat(details).hasSize(2);
            assertThat(details).extracting(OrderDetail::getOrgMemberId)
                    .containsExactlyInAnyOrder(10L, 20L);
        }

        @Test
        @DisplayName("Order 2의 상세 목록을 조회하면 해당 주문의 상세 1개가 반환된다")
        void findByOrderId_returnsDetailsForOrder2() {
            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderId(ORDER_2_ID);

            // then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getOrgMemberId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID로 조회하면 빈 목록이 반환된다")
        void findByOrderId_returnsEmptyList_whenOrderNotExists() {
            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderId(999L);

            // then
            assertThat(details).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 상세는 조회에서 제외된다")
        void findByOrderId_excludesDeletedDetails() {
            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderId(ORDER_1_ID);

            // then
            // 삭제된 상세(orgMemberId=99)는 제외되어 2개만 조회되어야 한다
            assertThat(details).hasSize(2);
            assertThat(details).extracting(OrderDetail::getOrgMemberId)
                    .doesNotContain(99L);
        }
    }

    @Nested
    @DisplayName("findByOrderIdIn 테스트")
    class FindByOrderIdInTest {

        @Test
        @DisplayName("여러 주문 ID로 일괄 조회하면 해당 주문들의 상세가 모두 반환된다")
        void findByOrderIdIn_returnsAllDetailsForGivenOrders() {
            // given
            List<Long> orderIds = List.of(ORDER_1_ID, ORDER_2_ID);

            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderIdIn(orderIds);

            // then
            // Order1 상세 2개 + Order2 상세 1개 = 총 3개
            assertThat(details).hasSize(3);
            assertThat(details).extracting(OrderDetail::getOrderId)
                    .containsOnly(ORDER_1_ID, ORDER_2_ID);
        }

        @Test
        @DisplayName("단일 주문 ID 목록으로 일괄 조회해도 정상 동작한다")
        void findByOrderIdIn_returnsSingleOrderDetails() {
            // given
            List<Long> orderIds = List.of(ORDER_3_ID);

            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderIdIn(orderIds);

            // then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getOrgMemberId()).isEqualTo(30L);
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID 목록으로 조회하면 빈 목록이 반환된다")
        void findByOrderIdIn_returnsEmptyList_whenNoOrdersMatch() {
            // given
            List<Long> orderIds = List.of(888L, 999L);

            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderIdIn(orderIds);

            // then
            assertThat(details).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 일괄 조회 시 소프트 삭제된 상세는 제외된다")
        void findByOrderIdIn_excludesDeletedDetails() {
            // given
            List<Long> orderIds = List.of(ORDER_1_ID, ORDER_2_ID, ORDER_3_ID);

            // when
            List<OrderDetail> details = orderDetailRepository.findByOrderIdIn(orderIds);

            // then
            // Order1 활성 2개 + Order2 1개 + Order3 1개 = 4개 (삭제된 1개 제외)
            assertThat(details).hasSize(4);
            assertThat(details).extracting(OrderDetail::getOrgMemberId)
                    .doesNotContain(99L);
        }
    }
}
