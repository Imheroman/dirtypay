package com.dirtypay.domain.order.repository;

import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderRepository} 단위 테스트.
 *
 * <p>라운드별 주문 조회, 세션·라운드 금액 합산,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    private Session session;
    private Round round1;
    private Round round2;
    private Order order1;
    private Order order2;
    private Order order3;
    private Order deletedOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        roundRepository.deleteAll();
        sessionRepository.deleteAll();

        // Session 생성
        session = sessionRepository.save(Session.builder()
                .title("테스트 세션")
                .description("주문 테스트용 세션")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .ownerId(1L)
                .build());

        // Round 생성 (session에 속한 2개의 라운드)
        round1 = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("1차 식사")
                .place("식당A")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.OPEN)
                .sortOrder(1)
                .build());

        round2 = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("2차 카페")
                .place("카페B")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.OPEN)
                .sortOrder(2)
                .build());

        // Order 생성 - round1에 2건, round2에 1건
        order1 = orderRepository.save(Order.builder()
                .roundId(round1.getId())
                .groupId(1L)
                .groupName("기본 그룹")
                .menuId(100L)
                .menuName("메뉴A")
                .menuPrice(new BigDecimal("10000.00"))
                .quantity(2)
                .totalPrice(new BigDecimal("20000.00"))
                .build());

        order2 = orderRepository.save(Order.builder()
                .roundId(round1.getId())
                .groupId(1L)
                .groupName("기본 그룹")
                .menuId(200L)
                .menuName("메뉴B")
                .menuPrice(new BigDecimal("15000.00"))
                .quantity(1)
                .totalPrice(new BigDecimal("15000.00"))
                .build());

        order3 = orderRepository.save(Order.builder()
                .roundId(round2.getId())
                .groupId(1L)
                .groupName("기본 그룹")
                .menuId(100L)
                .menuName("메뉴A")
                .menuPrice(new BigDecimal("10000.00"))
                .quantity(3)
                .totalPrice(new BigDecimal("30000.00"))
                .build());

        // 삭제된 주문 (soft delete)
        deletedOrder = orderRepository.save(Order.builder()
                .roundId(round1.getId())
                .groupId(1L)
                .groupName("기본 그룹")
                .menuId(300L)
                .menuName("메뉴C")
                .menuPrice(new BigDecimal("5000.00"))
                .quantity(1)
                .totalPrice(new BigDecimal("5000.00"))
                .build());
        deletedOrder.delete();
        orderRepository.save(deletedOrder);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByRoundId: 라운드별 주문 목록을 조회한다")
    void findByRoundId_returnsOrdersForRound() {
        // when
        List<Order> round1Orders = orderRepository.findByRoundId(round1.getId());
        List<Order> round2Orders = orderRepository.findByRoundId(round2.getId());

        // then
        assertThat(round1Orders).hasSize(2);
        assertThat(round1Orders).extracting(Order::getMenuId)
                .containsExactlyInAnyOrder(100L, 200L);

        assertThat(round2Orders).hasSize(1);
        assertThat(round2Orders.get(0).getMenuId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("sumTotalPriceBySessionId: 세션에 속한 모든 주문의 총 금액을 합산한다")
    void sumTotalPriceBySessionId_returnsTotalSum() {
        // when
        BigDecimal totalPrice = orderRepository.sumTotalPriceBySessionId(session.getId());

        // then - order1(20000) + order2(15000) + order3(30000) = 65000
        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("65000.00"));
    }

    @Test
    @DisplayName("sumTotalPriceBySessionId: 주문이 없는 세션은 0을 반환한다")
    void sumTotalPriceBySessionId_returnsZeroWhenNoOrders() {
        // given
        Session emptySession = sessionRepository.save(Session.builder()
                .title("빈 세션")
                .ownerId(2L)
                .build());

        roundRepository.save(Round.builder()
                .sessionId(emptySession.getId())
                .title("빈 라운드")
                .status(RoundStatus.OPEN)
                .sortOrder(1)
                .build());

        entityManager.flush();
        entityManager.clear();

        // when
        BigDecimal totalPrice = orderRepository.sumTotalPriceBySessionId(emptySession.getId());

        // then
        assertThat(totalPrice).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumTotalPriceByRoundId: 라운드에 속한 주문의 총 금액을 합산한다")
    void sumTotalPriceByRoundId_returnsTotalSum() {
        // when
        BigDecimal round1Total = orderRepository.sumTotalPriceByRoundId(round1.getId());
        BigDecimal round2Total = orderRepository.sumTotalPriceByRoundId(round2.getId());

        // then
        assertThat(round1Total).isEqualByComparingTo(new BigDecimal("35000.00"));
        assertThat(round2Total).isEqualByComparingTo(new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("@SQLRestriction: 삭제된 주문은 조회에서 제외된다")
    void sqlRestriction_excludesDeletedOrders() {
        // when
        List<Order> round1Orders = orderRepository.findByRoundId(round1.getId());
        BigDecimal round1Total = orderRepository.sumTotalPriceByRoundId(round1.getId());

        // then
        assertThat(round1Orders).hasSize(2);
        assertThat(round1Orders).extracting(Order::getMenuId)
                .doesNotContain(300L);
        assertThat(round1Total).isEqualByComparingTo(new BigDecimal("35000.00"));
    }
}
