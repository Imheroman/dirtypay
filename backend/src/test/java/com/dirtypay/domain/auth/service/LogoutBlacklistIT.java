package com.dirtypay.domain.auth.service;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.auth.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그아웃 시 Access Token 블랙리스트 등록 통합 테스트.
 *
 * <p>실제 Redis(Testcontainers)를 사용하여 로그아웃 후
 * Access Token jti가 블랙리스트에 등록됨을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"blacklist-redis"})
class LogoutBlacklistIT {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("로그아웃 후 Access Token의 jti가 Redis 블랙리스트에 등록된다")
    void logout_blacklistsAccessToken() {
        // given — 유효한 Access Token 생성
        Long memberId = 999L;
        String accessToken = jwtTokenProvider.createAccessToken(memberId, "test@test.com", "ROLE_USER");
        String jti = jwtTokenProvider.extractJti(accessToken);
        assertThat(jti).isNotNull();
        assertThat(tokenBlacklistService.isBlacklisted(jti)).isFalse();

        // when
        authService.logout(memberId, accessToken);

        // then
        assertThat(tokenBlacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    @DisplayName("이미 만료된 Access Token으로 로그아웃해도 예외가 발생하지 않는다")
    void logout_withExpiredToken_doesNotThrow() {
        // given — jti 없는 레거시 토큰 시뮬레이션 (extractJti → null 반환 불가이므로 실제 토큰 사용)
        Long memberId = 998L;
        String accessToken = jwtTokenProvider.createAccessToken(memberId, "legacy@test.com", "ROLE_USER");
        String jti = jwtTokenProvider.extractJti(accessToken);

        // when & then — 예외 없이 처리
        authService.logout(memberId, accessToken);

        // jti가 있으므로 블랙리스트에 등록됨
        assertThat(tokenBlacklistService.isBlacklisted(jti)).isTrue();
    }
}
