package com.dirtypay.domain.settlement.repository;

import com.dirtypay.domain.settlement.entity.SettlementPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 정산 완료 상태 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, Long> {

    /**
     * 세션 ID와 멤버 ID로 정산 완료 상태를 조회한다.
     *
     * @param sessionId   세션 ID
     * @param orgMemberId 멤버 ID
     * @return 정산 완료 상태 Optional
     */
    Optional<SettlementPayment> findBySessionIdAndOrgMemberId(Long sessionId, Long orgMemberId);

    /**
     * 세션 ID로 정산 완료 상태 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 정산 완료 상태 목록
     */
    List<SettlementPayment> findBySessionId(Long sessionId);
}
