package com.dirtypay.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Member CRUD 플로우 통합 테스트.
 *
 * <p>회원 조회, 수정, 삭제 및 인증 검증을 수행한다.
 * 각 테스트는 @Order에 따라 순서 의존적으로 실행되며,
 * 회원가입 → 조회 → 수정 → 삭제 → 삭제 후 조회 흐름을 검증한다.</p>
 *
 * <p>삭제 후 조회(Order 9)는 별도 조회자(observer) 계정을 사용한다.
 * Soft Delete된 회원의 JWT 토큰으로 요청 시 JwtAuthenticationFilter에서
 * CustomUserDetailsService가 EntityNotFoundException을 발생시켜 필터 체인 오류가
 * 발생하므로, 삭제되지 않은 별도 계정의 토큰으로 삭제된 회원 ID를 조회한다.</p>
 *
 * <p>IDOR 인가 검증(Order 6~7): observer 토큰으로 타인 리소스에 PUT/DELETE 시도 → 403 Forbidden.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class MemberCrudIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "member-crud-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String NAME = "크러드테스터";

    /** soft delete 후 타인 조회 검증에 사용할 별도 관찰자 계정 */
    private static final String OBSERVER_EMAIL = "member-crud-observer@example.com";
    private static final String OBSERVER_PASSWORD = "Password1!";
    private static final String OBSERVER_NAME = "관찰자";

    /**
     * 테스트 전체에서 공유되는 액세스 토큰.
     * Order(1) setup_signupAndGetToken 에서 초기화된다.
     */
    private String accessToken;

    /**
     * 테스트 전체에서 공유되는 회원 ID.
     * Order(1) setup_signupAndGetToken 에서 초기화된다.
     */
    private Long memberId;

    /**
     * soft delete 후 타인 조회에 사용하는 관찰자 계정 토큰.
     * Order(1) setup_signupAndGetToken 에서 초기화된다.
     */
    private String observerToken;

    // ========================
    // 사전 준비: 회원가입 및 ID 확보
    // ========================

    @Test
    @Order(1)
    @DisplayName("1. 사전 준비 - 회원가입 후 토큰 및 회원 ID 획득")
    void setup_signupAndGetToken() throws Exception {
        // Given: 신규 회원 정보
        // When: 회원가입 후 로그인 토큰 획득
        this.accessToken = signup(EMAIL, PASSWORD, NAME);

        // Then: 토큰이 정상적으로 발급되어야 한다
        Assertions.assertNotNull(accessToken, "액세스 토큰이 발급되어야 한다");

        // Given: 로그인된 상태에서 로그인 응답으로 회원 ID 추출
        String loginBody = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, EMAIL, PASSWORD);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Then: 로그인 응답의 data.user.id 에서 회원 ID 추출
        this.memberId = parseData(result).get("user").get("id").asLong();
        Assertions.assertNotNull(memberId, "회원 ID가 정상적으로 조회되어야 한다");

        // 삭제 후 타인 조회 검증에 사용할 관찰자 계정 생성
        this.observerToken = signup(OBSERVER_EMAIL, OBSERVER_PASSWORD, OBSERVER_NAME);
        Assertions.assertNotNull(observerToken, "관찰자 토큰이 발급되어야 한다");
    }

    // ========================
    // 조회 (GET /api/users/{id})
    // ========================

    @Test
    @Order(2)
    @DisplayName("2. 유효한 회원 조회 → 200 OK, 회원 데이터 반환")
    void getMember_validId_returns200WithMemberData() throws Exception {
        // Given: 가입된 회원 ID와 유효한 액세스 토큰
        // When: GET /api/users/{id} 요청
        mockMvc.perform(get("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", accessToken)))
                // Then: 200 OK, 회원 정보가 포함된 응답
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.name").value(NAME));
    }

    @Test
    @Order(3)
    @DisplayName("3. 존재하지 않는 회원 조회 → 404 NOT_FOUND")
    void getMember_nonExistentId_returns404() throws Exception {
        // Given: 존재하지 않는 회원 ID (Long 최대값 사용)
        long nonExistentId = Long.MAX_VALUE;

        // When: GET /api/users/{id} 요청
        mockMvc.perform(get("/api/users/{id}", nonExistentId)
                        .cookie(new Cookie("access_token", accessToken)))
                // Then: 404 NOT_FOUND, 에러 코드 MEMBER_001
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_001"));
    }

    // ========================
    // 수정 (PUT /api/users/{id})
    // ========================

    @Test
    @Order(4)
    @DisplayName("4. 유효한 수정 요청 → 200 OK, 수정된 데이터 반환")
    void updateMember_validRequest_returns200WithUpdatedData() throws Exception {
        // Given: 변경할 이름과 프로필 이미지
        String updatedName = "수정된이름";
        String updatedProfileImage = "https://example.com/profile.jpg";
        String requestBody = String.format("""
                {
                    "name": "%s",
                    "profileImage": "%s"
                }
                """, updatedName, updatedProfileImage);

        // When: PUT /api/users/{id} 요청
        mockMvc.perform(put("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Then: 200 OK, 수정된 회원 정보 반환
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(memberId))
                .andExpect(jsonPath("$.data.name").value(updatedName))
                .andExpect(jsonPath("$.data.profileImage").value(updatedProfileImage));
    }

    @Test
    @Order(5)
    @DisplayName("5. 이름 null/blank 수정 요청 → 400 BAD_REQUEST")
    void updateMember_blankName_returns400() throws Exception {
        // Given: name 필드가 빈 문자열인 잘못된 요청
        String requestBody = """
                {
                    "name": ""
                }
                """;

        // When: PUT /api/users/{id} 요청
        mockMvc.perform(put("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Then: 400 BAD_REQUEST, 유효성 검증 실패
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========================
    // IDOR 인가 검증 (PUT/DELETE - 타인 리소스 접근)
    // ========================

    @Test
    @Order(6)
    @DisplayName("6. 타인 토큰으로 회원 정보 수정 시도 → 403 Forbidden (IDOR 차단)")
    void updateMember_anotherUserToken_returns403() throws Exception {
        // Given: 관찰자(observer) 계정 토큰으로 다른 회원(memberId)의 정보 수정 시도
        String requestBody = """
                {
                    "name": "해킹시도",
                    "profileImage": "https://evil.example.com/hacked.jpg"
                }
                """;

        // When: PUT /api/users/{memberId} with observerToken (다른 사용자의 토큰)
        mockMvc.perform(put("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", observerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Then: 403 Forbidden - 타인 리소스 접근 차단
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("7. 타인 토큰으로 회원 삭제 시도 → 403 Forbidden (IDOR 차단)")
    void deleteMember_anotherUserToken_returns403() throws Exception {
        // Given: 관찰자(observer) 계정 토큰으로 다른 회원(memberId)의 삭제 시도
        // When: DELETE /api/users/{memberId} with observerToken (다른 사용자의 토큰)
        mockMvc.perform(delete("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", observerToken)))
                // Then: 403 Forbidden - 타인 리소스 접근 차단
                .andExpect(status().isForbidden());
    }

    // ========================
    // 삭제 (DELETE /api/users/{id})
    // ========================

    @Test
    @Order(8)
    @DisplayName("8. 유효한 삭제 요청 → 200 OK (Soft Delete)")
    void deleteMember_validId_returns200() throws Exception {
        // Given: 가입된 회원 ID와 유효한 액세스 토큰
        // When: DELETE /api/users/{id} 요청
        mockMvc.perform(delete("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", accessToken)))
                // Then: 200 OK, soft delete 성공
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(9)
    @DisplayName("9. 삭제 후 타인이 삭제된 회원 조회 → 404 (soft delete 필터링)")
    void getMember_afterDelete_returns404() throws Exception {
        // Given: soft delete 처리된 회원 ID (Order(8) 에서 삭제됨)
        //        삭제된 회원의 토큰은 JwtAuthenticationFilter에서 CustomUserDetailsService가
        //        @SQLRestriction 필터로 인해 EntityNotFoundException을 발생시키므로,
        //        삭제되지 않은 관찰자 계정의 토큰으로 삭제된 회원 ID를 조회한다.
        // When: GET /api/users/{id} 로 삭제된 회원 조회 시도 (관찰자 토큰 사용)
        mockMvc.perform(get("/api/users/{id}", memberId)
                        .cookie(new Cookie("access_token", observerToken)))
                // Then: @SQLRestriction에 의해 deleted_date IS NULL 조건 적용 → 404
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_001"));
    }

    // ========================
    // 인증 필요 검증
    // ========================

    @Test
    @Order(10)
    @DisplayName("10. 토큰 없이 보호 엔드포인트 접근 → 401 UNAUTHORIZED")
    void protectedEndpoint_noToken_returns401() throws Exception {
        // Given: 인증 토큰 없이 회원 조회 요청
        // When: GET /api/users/{id} 쿠키 없이 요청
        mockMvc.perform(get("/api/users/{id}", memberId))
                // Then: 401 UNAUTHORIZED
                .andExpect(status().isUnauthorized());
    }
}
