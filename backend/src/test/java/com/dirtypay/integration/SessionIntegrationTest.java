package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 세션 CRUD + 집계 필드 통합 테스트.
 *
 * <p>세션 생성, 조회, 수정, 삭제 및 비소유자 접근 차단, 집계 필드 정확성을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class SessionIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "session-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "session-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long storeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "세션테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
    }

    // === 세션 CRUD ===

    @Test
    @Order(2)
    @DisplayName("1. 세션 생성 성공")
    void createSession_success() throws Exception {
        String body = """
                {
                    "title": "세션 테스트",
                    "description": "통합 테스트",
                    "startDate": "2026-01-01",
                    "endDate": "2026-12-31"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("세션 테스트"))
                .andReturn();

        this.sessionId = parseData(result).get("id").asLong();
    }

    @Test
    @Order(3)
    @DisplayName("2. 세션 조회 - 초기 집계 필드 (memberCount=1(소유자), roundCount=0, totalAmount=0)")
    void getSession_initialAggregates() throws Exception {
        mockMvc.perform(get("/api/sessions/{id}", sessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sessionId))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.roundCount").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    @Order(4)
    @DisplayName("3. 세션 목록 조회 - 자신의 세션만 반환")
    void listSessions_ownOnly() throws Exception {
        // 타인이 세션 생성
        createSession(otherToken, "타인의 세션");

        // 내 세션 목록 조회 시 내 세션만 반환
        MvcResult result = mockMvc.perform(get("/api/sessions")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode session : data) {
            assertThat(session.get("title").asText())
                    .as("자신의 세션만 조회되어야 한다")
                    .isEqualTo("세션 테스트");
        }
    }

    @Test
    @Order(5)
    @DisplayName("4. 세션 수정 성공")
    void updateSession_success() throws Exception {
        String body = """
                {
                    "title": "수정된 세션",
                    "description": "수정됨"
                }
                """;

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 세션"));
    }

    // === 비소유자 접근 차단 ===

    @Test
    @Order(6)
    @DisplayName("5. 비소유자 세션 수정 시 403")
    void updateSession_nonOwner() throws Exception {
        String body = """
                {
                    "title": "해킹 시도"
                }
                """;

        mockMvc.perform(put("/api/sessions/{id}", sessionId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    @Test
    @Order(7)
    @DisplayName("6. 비소유자 세션 삭제 시 403")
    void deleteSession_nonOwner() throws Exception {
        mockMvc.perform(delete("/api/sessions/{id}", sessionId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    @Test
    @Order(8)
    @DisplayName("7. 존재하지 않는 세션 조회 시 404")
    void getSession_notFound() throws Exception {
        mockMvc.perform(get("/api/sessions/{id}", 999999)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SESSION_001"));
    }

    // === 집계 필드 검증 ===

    @Test
    @Order(9)
    @DisplayName("8. 데이터 추가 후 집계 필드 검증")
    void getSession_aggregatesAfterData() throws Exception {
        // 노드 + 멤버 2명 추가
        createRootNode(accessToken, sessionId, "전체");
        createMember(accessToken, sessionId, "멤버1");
        createMember(accessToken, sessionId, "멤버2");

        // 라운드 생성
        this.storeId = createStore(accessToken, "세션 테스트 매장");
        createStoreMenu(accessToken, storeId, "기본메뉴", 10000);
        Long roundId = createRound(accessToken, sessionId, "1차", storeId);

        // 메뉴 + 주문
        Long menuId = createStoreMenu(accessToken, storeId, "삼겹살", 15000);
        createOrder(accessToken, roundId, menuId, 2,
                getMemberIdsForSession(sessionId));

        // 집계 검증
        mockMvc.perform(get("/api/sessions/{id}", sessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(3))
                .andExpect(jsonPath("$.data.roundCount").value(1));
    }

    @Test
    @Order(10)
    @DisplayName("9. 인증 없이 세션 생성 시 401")
    void createSession_noAuth() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "무인증"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("10. 세션 삭제 후 조회 시 404")
    void deleteSession_thenGet() throws Exception {
        // 새 세션 생성 후 삭제
        Long tempSessionId = createSession(accessToken, "삭제용 세션");

        mockMvc.perform(delete("/api/sessions/{id}", tempSessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/sessions/{id}", tempSessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_001"));
    }

    /**
     * 세션의 모든 멤버 ID를 조회한다.
     */
    private Long[] getMemberIdsForSession(Long sessionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/members", sessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        Long[] ids = new Long[data.size()];
        for (int i = 0; i < data.size(); i++) {
            ids[i] = data.get(i).get("id").asLong();
        }
        return ids;
    }
}
