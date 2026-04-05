package com.dirtypay.domain.auth.repository;

import com.dirtypay.domain.auth.entity.RefreshToken;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshTokenRepository 통합 테스트.
 *
 * <p>@DataJpaTest 환경에서 실제 JPA 동작을 검증한다.
 * findByToken, findByMemberId, deleteAllByMemberId, deleteByToken,
 * deleteAllExpiredTokens, existsByMemberId, existsByToken 등의
 * 커스텀 쿼리 메서드가 올바르게 동작하는지 확인한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 픽스처 상수
    private static final Long MEMBER_ID_1 = 1L;
    private static final Long MEMBER_ID_2 = 2L;
    private static final Long MEMBER_ID_NO_TOKEN = 99L;

    private RefreshToken validToken1;
    private RefreshToken validToken2;
    private RefreshToken expiredToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // member 1의 유효한 토큰
        validToken1 = refreshTokenRepository.save(RefreshToken.builder()
                .memberId(MEMBER_ID_1)
                .token("valid-token-member1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        // member 1의 두 번째 유효한 토큰 (다중 기기 시나리오)
        validToken2 = refreshTokenRepository.save(RefreshToken.builder()
                .memberId(MEMBER_ID_1)
                .token("valid-token-member1-second")
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build());

        // member 2의 만료된 토큰
        expiredToken = refreshTokenRepository.save(RefreshToken.builder()
                .memberId(MEMBER_ID_2)
                .token("expired-token-member2")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build());

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByToken 테스트")
    class FindByTokenTest {

        @Test
        @DisplayName("존재하는 토큰으로 조회하면 RefreshToken을 반환한다")
        void findByToken_success() {
            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByToken("valid-token-member1");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getToken()).isEqualTo("valid-token-member1");
            assertThat(found.get().getMemberId()).isEqualTo(MEMBER_ID_1);
        }

        @Test
        @DisplayName("존재하지 않는 토큰으로 조회하면 Optional.empty()를 반환한다")
        void findByToken_notFound_returnsEmpty() {
            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByToken("non-existing-token");

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMemberId 테스트")
    class FindByMemberIdTest {

        @Test
        @DisplayName("토큰이 있는 회원 ID로 조회하면 RefreshToken을 반환한다")
        void findByMemberId_success() {
            // given
            // member 2에는 토큰이 1개만 있으므로 유일하게 조회됨
            Optional<RefreshToken> found = refreshTokenRepository.findByMemberId(MEMBER_ID_2);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getMemberId()).isEqualTo(MEMBER_ID_2);
        }

        @Test
        @DisplayName("토큰이 없는 회원 ID로 조회하면 Optional.empty()를 반환한다")
        void findByMemberId_notFound_returnsEmpty() {
            // when
            Optional<RefreshToken> found = refreshTokenRepository.findByMemberId(MEMBER_ID_NO_TOKEN);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteAllByMemberId 테스트")
    class DeleteAllByMemberIdTest {

        @Test
        @DisplayName("회원 ID로 삭제하면 해당 회원의 모든 토큰이 삭제된다")
        void deleteAllByMemberId_deletesAllTokensForMember() {
            // given - member 1의 토큰이 2개 존재하는 상태

            // when
            refreshTokenRepository.deleteAllByMemberId(MEMBER_ID_1);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(refreshTokenRepository.findByToken("valid-token-member1")).isEmpty();
            assertThat(refreshTokenRepository.findByToken("valid-token-member1-second")).isEmpty();
        }

        @Test
        @DisplayName("회원 ID로 삭제해도 다른 회원의 토큰은 유지된다")
        void deleteAllByMemberId_doesNotAffectOtherMembers() {
            // when
            refreshTokenRepository.deleteAllByMemberId(MEMBER_ID_1);
            entityManager.flush();
            entityManager.clear();

            // then - member 2의 토큰은 유지되어야 함
            assertThat(refreshTokenRepository.findByMemberId(MEMBER_ID_2)).isPresent();
        }

        @Test
        @DisplayName("토큰이 없는 회원 ID로 삭제해도 예외가 발생하지 않는다")
        void deleteAllByMemberId_noTokens_noException() {
            // when & then - 예외 없이 정상 실행
            refreshTokenRepository.deleteAllByMemberId(MEMBER_ID_NO_TOKEN);
            entityManager.flush();
        }
    }

    @Nested
    @DisplayName("deleteByToken 테스트")
    class DeleteByTokenTest {

        @Test
        @DisplayName("토큰 값으로 삭제하면 해당 토큰만 삭제된다")
        void deleteByToken_deletesSpecificToken() {
            // when
            refreshTokenRepository.deleteByToken("valid-token-member1");
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(refreshTokenRepository.findByToken("valid-token-member1")).isEmpty();
        }

        @Test
        @DisplayName("특정 토큰 삭제 후 동일 회원의 다른 토큰은 유지된다")
        void deleteByToken_doesNotAffectOtherTokens() {
            // when
            refreshTokenRepository.deleteByToken("valid-token-member1");
            entityManager.flush();
            entityManager.clear();

            // then - member1의 두 번째 토큰은 유지되어야 함
            assertThat(refreshTokenRepository.findByToken("valid-token-member1-second")).isPresent();
        }
    }

    @Nested
    @DisplayName("deleteAllExpiredTokens 테스트")
    class DeleteAllExpiredTokensTest {

        @Test
        @DisplayName("만료된 토큰 삭제 쿼리 실행 후 만료 토큰이 삭제된다")
        void deleteAllExpiredTokens_expiredTokensAreDeleted() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            refreshTokenRepository.deleteAllExpiredTokens(now);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(refreshTokenRepository.findByToken("expired-token-member2")).isEmpty();
        }

        @Test
        @DisplayName("만료된 토큰 삭제 후 유효한 토큰은 유지된다")
        void deleteAllExpiredTokens_validTokensArePreserved() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            refreshTokenRepository.deleteAllExpiredTokens(now);
            entityManager.flush();
            entityManager.clear();

            // then - 유효한 토큰은 삭제되지 않아야 함
            assertThat(refreshTokenRepository.findByToken("valid-token-member1")).isPresent();
            assertThat(refreshTokenRepository.findByToken("valid-token-member1-second")).isPresent();
        }

        @Test
        @DisplayName("만료된 토큰 삭제 시 삭제된 토큰 수를 반환한다")
        void deleteAllExpiredTokens_returnsDeletedCount() {
            // given
            LocalDateTime now = LocalDateTime.now();

            // when
            int deletedCount = refreshTokenRepository.deleteAllExpiredTokens(now);

            // then
            assertThat(deletedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("existsByMemberId 테스트")
    class ExistsByMemberIdTest {

        @Test
        @DisplayName("토큰이 있는 회원 ID이면 true를 반환한다")
        void existsByMemberId_exists_returnsTrue() {
            // when
            boolean exists = refreshTokenRepository.existsByMemberId(MEMBER_ID_1);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("토큰이 없는 회원 ID이면 false를 반환한다")
        void existsByMemberId_notExists_returnsFalse() {
            // when
            boolean exists = refreshTokenRepository.existsByMemberId(MEMBER_ID_NO_TOKEN);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByToken 테스트")
    class ExistsByTokenTest {

        @Test
        @DisplayName("존재하는 토큰 값이면 true를 반환한다")
        void existsByToken_exists_returnsTrue() {
            // when
            boolean exists = refreshTokenRepository.existsByToken("valid-token-member1");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 토큰 값이면 false를 반환한다")
        void existsByToken_notExists_returnsFalse() {
            // when
            boolean exists = refreshTokenRepository.existsByToken("non-existing-token");

            // then
            assertThat(exists).isFalse();
        }
    }
}
