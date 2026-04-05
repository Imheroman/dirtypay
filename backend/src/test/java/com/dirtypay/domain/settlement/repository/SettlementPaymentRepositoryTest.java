package com.dirtypay.domain.settlement.repository;

import com.dirtypay.domain.settlement.entity.SettlementPayment;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SettlementPaymentRepository} 단위 테스트.
 *
 * <p>세션+멤버 조합 조회, 세션 전체 정산 목록 조회,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class SettlementPaymentRepositoryTest {

    @Autowired
    private SettlementPaymentRepository settlementPaymentRepository;

    @Autowired
    private EntityManager entityManager;

    private SettlementPayment payment1;
    private SettlementPayment payment2;
    private SettlementPayment paymentInOtherSession;
    private SettlementPayment deletedPayment;

    private static final Long SESSION_ID = 1L;
    private static final Long OTHER_SESSION_ID = 2L;
    private static final Long ORG_MEMBER_ID_1 = 100L;
    private static final Long ORG_MEMBER_ID_2 = 200L;
    private static final Long ORG_MEMBER_ID_3 = 300L;

    @BeforeEach
    void setUp() {
        settlementPaymentRepository.deleteAll();

        // SESSION_ID에 속한 정산 2건
        payment1 = settlementPaymentRepository.save(SettlementPayment.builder()
                .sessionId(SESSION_ID)
                .orgMemberId(ORG_MEMBER_ID_1)
                .build());

        payment2 = settlementPaymentRepository.save(SettlementPayment.builder()
                .sessionId(SESSION_ID)
                .orgMemberId(ORG_MEMBER_ID_2)
                .build());
        payment2.updatePayment(BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));
        settlementPaymentRepository.save(payment2);

        // 다른 세션에 속한 정산
        paymentInOtherSession = settlementPaymentRepository.save(SettlementPayment.builder()
                .sessionId(OTHER_SESSION_ID)
                .orgMemberId(ORG_MEMBER_ID_1)
                .build());

        // 소프트 삭제된 정산
        deletedPayment = settlementPaymentRepository.save(SettlementPayment.builder()
                .sessionId(SESSION_ID)
                .orgMemberId(ORG_MEMBER_ID_3)
                .build());
        deletedPayment.delete();
        settlementPaymentRepository.save(deletedPayment);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findBySessionIdAndOrgMemberId: 세션+멤버 조합으로 정산 상태를 조회한다")
    void findBySessionIdAndOrgMemberId_returnsTargetPayment() {
        // when
        Optional<SettlementPayment> found = settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(SESSION_ID, ORG_MEMBER_ID_1);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSessionId()).isEqualTo(SESSION_ID);
        assertThat(found.get().getOrgMemberId()).isEqualTo(ORG_MEMBER_ID_1);
        assertThat(found.get().isPaid()).isFalse();
    }

    @Test
    @DisplayName("findBySessionIdAndOrgMemberId: 완납된 정산도 정상 조회된다")
    void findBySessionIdAndOrgMemberId_returnsPaidPayment() {
        // when
        Optional<SettlementPayment> found = settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(SESSION_ID, ORG_MEMBER_ID_2);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().isPaid()).isTrue();
        assertThat(found.get().getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("findBySessionIdAndOrgMemberId: 존재하지 않는 조합이면 Optional.empty()를 반환한다")
    void findBySessionIdAndOrgMemberId_returnsEmptyWhenNotFound() {
        // when
        Optional<SettlementPayment> found = settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(SESSION_ID, 9999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findBySessionId: 세션 ID로 모든 정산 목록을 조회한다")
    void findBySessionId_returnsAllPaymentsInSession() {
        // when
        List<SettlementPayment> payments = settlementPaymentRepository.findBySessionId(SESSION_ID);

        // then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SettlementPayment::getOrgMemberId)
                .containsExactlyInAnyOrder(ORG_MEMBER_ID_1, ORG_MEMBER_ID_2);
    }

    @Test
    @DisplayName("findBySessionId: 존재하지 않는 세션 ID이면 빈 목록을 반환한다")
    void findBySessionId_returnsEmptyWhenSessionNotFound() {
        // when
        List<SettlementPayment> payments = settlementPaymentRepository.findBySessionId(9999L);

        // then
        assertThat(payments).isEmpty();
    }

    @Test
    @DisplayName("@SQLRestriction: 소프트 삭제된 정산은 모든 조회에서 제외된다")
    void sqlRestriction_excludesDeletedPayments() {
        // when
        List<SettlementPayment> sessionPayments = settlementPaymentRepository.findBySessionId(SESSION_ID);
        Optional<SettlementPayment> deletedFound = settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(SESSION_ID, ORG_MEMBER_ID_3);

        // then - 삭제된 orgMemberId=300 의 정산은 결과에 포함되지 않아야 한다
        assertThat(sessionPayments).extracting(SettlementPayment::getOrgMemberId)
                .doesNotContain(ORG_MEMBER_ID_3);
        assertThat(deletedFound).isEmpty();
    }
}
