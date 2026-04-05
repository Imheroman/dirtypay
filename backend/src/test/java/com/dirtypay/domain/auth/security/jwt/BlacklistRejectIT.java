package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 블랙리스트 등록된 Access Token 거부 통합 테스트.
 *
 * <p>로그아웃 후 블랙리스트에 등록된 Access Token으로 API를 호출하면
 * {@code JwtAuthenticationFilter}가 401 Unauthorized를 반환함을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"blacklist-redis"})
class BlacklistRejectIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("블랙리스트에 등록된 Access Token으로 API 호출 시 401을 반환한다")
    void request_withBlacklistedToken_returns401() throws Exception {
        // given — Access Token 생성 및 블랙리스트 등록
        String accessToken = jwtTokenProvider.createAccessToken(1L, "user@test.com", "ROLE_USER");
        String jti = jwtTokenProvider.extractJti(accessToken);
        long remainingMillis = jwtTokenProvider.getRemainingExpiryMillis(accessToken);
        tokenBlacklistService.blacklistAccessToken(jti, remainingMillis);

        // when & then — 블랙리스트 토큰으로 인증이 필요한 API 호출 → 401
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("블랙리스트에 없는 유효한 Access Token으로 API 호출 시 401이 아닌 응답을 반환한다")
    void request_withValidToken_doesNotReturn401() throws Exception {
        // given — 블랙리스트에 없는 유효한 Access Token
        String accessToken = jwtTokenProvider.createAccessToken(2L, "valid@test.com", "ROLE_USER");

        // when & then — 401이 아닌 응답 (인증은 통과, 리소스 접근 여부와 무관)
        int statusCode = mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(statusCode).isNotEqualTo(401);
    }
}
