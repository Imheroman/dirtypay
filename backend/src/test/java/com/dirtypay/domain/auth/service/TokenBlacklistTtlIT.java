package com.dirtypay.domain.auth.service;

import com.dirtypay.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TokenBlacklistService} Redis TTL 통합 테스트.
 *
 * <p>실제 Redis(Testcontainers)를 사용하여 TTL 만료 후 블랙리스트 해제 동작을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"blacklist-redis"})
@DisplayName("TokenBlacklistService TTL 통합 테스트")
class TokenBlacklistTtlIT {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Nested
    @DisplayName("TTL 만료 동작 테스트")
    class TtlExpiryTest {

        @Test
        @DisplayName("TTL 만료 후 블랙리스트에서 자동 해제된다")
        void blacklistAccessToken_TTL만료후_블랙리스트해제() throws InterruptedException {
            // given
            String jti = UUID.randomUUID().toString();
            long ttlMillis = 1000L;

            // when - 등록
            tokenBlacklistService.blacklistAccessToken(jti, ttlMillis);

            // then - 등록 직후 블랙리스트 확인
            assertThat(tokenBlacklistService.isBlacklisted(jti)).isTrue();

            // when - TTL 만료 대기 (1.5초로 오차 범위 확보)
            Thread.sleep(1500L);

            // then - 만료 후 블랙리스트 해제 확인
            assertThat(tokenBlacklistService.isBlacklisted(jti)).isFalse();
        }

        @Test
        @DisplayName("remainingMillis가 0 이하이면 Redis에 키가 등록되지 않는다")
        void blacklistAccessToken_만료토큰_Redis키미등록() {
            // given
            String jti = UUID.randomUUID().toString();

            // when
            tokenBlacklistService.blacklistAccessToken(jti, 0L);

            // then
            assertThat(tokenBlacklistService.isBlacklisted(jti)).isFalse();
        }
    }
}
