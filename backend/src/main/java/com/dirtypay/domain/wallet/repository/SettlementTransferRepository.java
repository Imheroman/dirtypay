package com.dirtypay.domain.wallet.repository;

import com.dirtypay.domain.wallet.entity.SettlementTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 정산 송금 리포지토리.
 *
 * <p>정산 송금 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface SettlementTransferRepository extends JpaRepository<SettlementTransfer, Long> {

    /**
     * 세션 ID와 조직 멤버 ID로 정산 송금을 조회한다.
     *
     * @param sessionId   세션 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 정산 송금 Optional
     */
    Optional<SettlementTransfer> findBySessionIdAndOrgMemberId(Long sessionId, Long orgMemberId);

    /**
     * 세션 ID로 정산 송금 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 해당 세션의 정산 송금 목록
     */
    List<SettlementTransfer> findBySessionId(Long sessionId);

    /**
     * 세션 ID와 조직 멤버 ID로 정산 송금 존재 여부를 확인한다.
     *
     * @param sessionId   세션 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 존재 여부
     */
    boolean existsBySessionIdAndOrgMemberId(Long sessionId, Long orgMemberId);
}
