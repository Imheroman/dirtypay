package com.dirtypay.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증/인가 통합 테스트.
 *
 * <p>회원가입, 로그인, 토큰 갱신, 로그아웃 및 보호 엔드포인트 접근 검증을 수행한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "auth-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String NAME = "인증테스터";

    private String accessToken;
    private String refreshToken;

    // === 회원가입 ===

    @Test
    @Order(1)
    @DisplayName("1. 회원가입 성공")
    void signup_success() throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, EMAIL, PASSWORD, NAME);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.name").value(NAME));
    }

    @Test
    @Order(2)
    @DisplayName("2. 중복 이메일 회원가입 실패")
    void signup_duplicateEmail() throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, EMAIL, PASSWORD, NAME);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    // === 로그인 ===

    @Test
    @Order(3)
    @DisplayName("3. 로그인 성공 - access_token 쿠키 반환")
    void login_success() throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, EMAIL, PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value(EMAIL))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(cookie().exists("access_token"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("access_token");
        assertThat(cookie).isNotNull();
        this.accessToken = cookie.getValue();
        this.refreshToken = parseData(result).get("refreshToken").asText();
    }

    @Test
    @Order(4)
    @DisplayName("4. 잘못된 비밀번호 로그인 실패")
    void login_wrongPassword() throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "wrong-password"
                }
                """, EMAIL);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(5)
    @DisplayName("5. 존재하지 않는 이메일 로그인 실패")
    void login_nonExistentEmail() throws Exception {
        String body = String.format("""
                {
                    "email": "nonexistent@example.com",
                    "password": "%s"
                }
                """, PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // === 보호 엔드포인트 접근 ===

    @Test
    @Order(6)
    @DisplayName("6. 토큰 없이 보호 엔드포인트 접근 시 401")
    void protectedEndpoint_noToken() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "테스트"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("7. 유효하지 않은 토큰으로 보호 엔드포인트 접근 시 401")
    void protectedEndpoint_invalidToken() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .cookie(new Cookie("access_token", "invalid-jwt-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "테스트"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // === 토큰 갱신 ===

    @Test
    @Order(8)
    @DisplayName("8. 리프레시 토큰으로 액세스 토큰 갱신")
    void refresh_success() throws Exception {
        String body = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(cookie().exists("access_token"))
                .andReturn();

        // 새 토큰으로 갱신
        Cookie cookie = result.getResponse().getCookie("access_token");
        assertThat(cookie).isNotNull();
        this.accessToken = cookie.getValue();
        this.refreshToken = parseData(result).get("refreshToken").asText();
    }

    // === 로그아웃 ===

    @Test
    @Order(9)
    @DisplayName("9. 로그아웃 성공")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(10)
    @DisplayName("10. 로그아웃 후 리프레시 토큰 사용 실패")
    void refresh_afterLogout() throws Exception {
        String body = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
