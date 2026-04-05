package com.dirtypay.domain.wallet.repository;

import com.dirtypay.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 지갑 거래 이력 리포지토리.
 *
 * <p>지갑 거래 이력 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * 멱등성 키로 거래 이력을 조회한다.
     *
     * @param idempotencyKey 조회할 멱등성 키
     * @return 거래 이력 Optional
     */
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * 지갑 ID로 거래 이력을 생성일 내림차순으로 페이징 조회한다.
     *
     * @param walletId 조회할 지갑 ID
     * @param pageable 페이징 정보
     * @return 거래 이력 Page
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedDateDesc(Long walletId, Pageable pageable);
}
