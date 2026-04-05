package com.dirtypay.domain.wallet.repository;

import com.dirtypay.domain.wallet.entity.Wallet;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link WalletRepository} 통합 테스트.
 *
 * <p>지갑 조회 메서드({@code findByMemberId}, {@code findById})의
 * 정상 조회 및 {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EntityManager entityManager;

    private Wallet activeWallet1;
    private Wallet activeWallet2;
    private Wallet deletedWallet;

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();

        activeWallet1 = walletRepository.save(Wallet.builder().memberId(1L).build());
        activeWallet2 = walletRepository.save(Wallet.builder().memberId(2L).build());

        // 소프트 삭제된 지갑 (memberId=999)
        deletedWallet = walletRepository.save(Wallet.builder().memberId(999L).build());
        deletedWallet.delete();
        walletRepository.save(deletedWallet);

        entityManager.flush();
        entityManager.clear();
    }

    // =========================================================
    // findByMemberId
    // =========================================================

    @Nested
    @DisplayName("findByMemberId 테스트")
    class FindByMemberIdTest {

        @Test
        @DisplayName("존재하는 memberId로 조회하면 지갑이 반환된다")
        void findByMemberId_returnsWallet_whenExists() {
            // when
            Optional<Wallet> result = walletRepository.findByMemberId(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 memberId로 조회하면 Optional.empty()가 반환된다")
        void findByMemberId_returnsEmpty_whenMemberIdNotExists() {
            // when
            Optional<Wallet> result = walletRepository.findByMemberId(9999L);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 지갑은 memberId로 조회되지 않는다")
        void findByMemberId_excludesDeletedWallet() {
            // when — memberId=999는 소프트 삭제된 지갑의 memberId
            Optional<Wallet> result = walletRepository.findByMemberId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // findById
    // =========================================================

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("존재하는 walletId로 조회하면 지갑이 반환된다")
        void findById_returnsWallet_whenExists() {
            // when
            Optional<Wallet> result = walletRepository.findById(activeWallet1.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(activeWallet1.getId());
            assertThat(result.get().getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 walletId로 조회하면 Optional.empty()가 반환된다")
        void findById_returnsEmpty_whenWalletIdNotExists() {
            // when
            Optional<Wallet> result = walletRepository.findById(Long.MAX_VALUE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 지갑은 walletId로 조회되지 않는다")
        void findById_excludesDeletedWallet() {
            // when — deletedWallet은 deletedDate가 설정된 소프트 삭제 지갑
            Optional<Wallet> result = walletRepository.findById(deletedWallet.getId());

            // then
            assertThat(result).isEmpty();
        }
    }
}
