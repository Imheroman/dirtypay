package com.dirtypay.domain.wallet.repository;

import com.dirtypay.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 지갑 리포지토리.
 *
 * <p>지갑 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * 회원 ID로 지갑을 조회한다.
     *
     * @param memberId 조회할 회원 ID
     * @return 지갑 Optional
     */
    Optional<Wallet> findByMemberId(Long memberId);

    /**
     * 회원 ID에 해당하는 지갑 존재 여부를 확인한다.
     *
     * @param memberId 확인할 회원 ID
     * @return 지갑 존재 여부
     */
    boolean existsByMemberId(Long memberId);
}
