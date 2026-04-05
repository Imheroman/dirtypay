package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 라운드 + 참여자 + 상태 변경 통합 테스트.
 *
 * <p>라운드 CRUD, 참여자 자동 초기화, 제외/포함, OPEN↔CLOSED 상태 전환을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class RoundIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "round-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "round-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long roundId;
    private Long participantId;
    private Long storeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "라운드테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
        this.sessionId = createSession(accessToken, "라운드 테스트 세션");
        this.storeId = createStore(accessToken, "라운드 테스트 매장");
        createStoreMenu(accessToken, storeId, "기본메뉴", 10000);

        createRootNode(accessToken, sessionId, "전체");
        createMember(accessToken, sessionId, "멤버1");
        createMember(accessToken, sessionId, "멤버2");
        createMember(accessToken, sessionId, "멤버3");
    }

    // === 라운드 CRUD ===

    @Test
    @Order(2)
    @DisplayName("1. 라운드 생성 + 참여자 자동 초기화 (참여자 수 = 멤버 수)")
    void createRound_withParticipants() throws Exception {
        String body = String.format("""
                {
                    "title": "1차 회식",
                    "place": "강남역",
                    "roundDate": "2026-03-01",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, storeId);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/rounds", sessionId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("1차 회식"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        this.roundId = parseData(result).get("id").asLong();

        // 참여자 수 = 멤버 수 (소유자 1명 + 수동 추가 3명 = 4명)
        mockMvc.perform(get("/api/rounds/{roundId}/participants", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    @Order(3)
    @DisplayName("2. 라운드 조회 - 상세 정보")
    void getRound_detail() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roundId))
                .andExpect(jsonPath("$.data.title").value("1차 회식"))
                .andExpect(jsonPath("$.data.place").value("강남역"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @Order(4)
    @DisplayName("3. 라운드 수정 - title 변경")
    void updateRound_success() throws Exception {
        String body = String.format("""
                {
                    "title": "수정된 회식",
                    "place": "역삼역",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, storeId);

        mockMvc.perform(put("/api/rounds/{roundId}", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 회식"));
    }

    // === 상태 변경 ===

    @Test
    @Order(5)
    @DisplayName("4. OPEN → CLOSED 변경")
    void changeStatus_openToClosed() throws Exception {
        closeRound(accessToken, roundId);
    }

    @Test
    @Order(6)
    @DisplayName("5. CLOSED → OPEN 변경 (양방향)")
    void changeStatus_closedToOpen() throws Exception {
        reopenRound(accessToken, roundId);
    }

    // === 참여자 제외/포함 ===

    @Test
    @Order(7)
    @DisplayName("6. 참여자 제외 - excluded=true")
    void excludeParticipant() throws Exception {
        // 참여자 목록에서 첫 번째 참여자 ID 획득
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/participants", roundId)
                        .cookie(authCookie(accessToken)))
                .andReturn();

        JsonNode participants = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        this.participantId = participants.get(0).get("id").asLong();

        mockMvc.perform(put("/api/rounds/{roundId}/participants/{participantId}/exclude",
                        roundId, participantId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excluded").value(true));
    }

    @Test
    @Order(8)
    @DisplayName("7. 참여자 포함 복원 - excluded=false")
    void includeParticipant() throws Exception {
        mockMvc.perform(put("/api/rounds/{roundId}/participants/{participantId}/include",
                        roundId, participantId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excluded").value(false));
    }

    // === 라운드 삭제 ===

    @Test
    @Order(9)
    @DisplayName("8. 라운드 삭제 - soft delete, 조회 시 404")
    void deleteRound_thenNotFound() throws Exception {
        // 삭제용 라운드 생성
        Long tempRoundId = createRound(accessToken, sessionId, "삭제용 라운드", storeId);

        mockMvc.perform(delete("/api/rounds/{roundId}", tempRoundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNoContent());

        // 조회 시 404
        mockMvc.perform(get("/api/rounds/{roundId}", tempRoundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROUND_001"));
    }

    // === 비소유자 접근 ===

    @Test
    @Order(10)
    @DisplayName("9. 비소유자 라운드 생성 시 403")
    void createRound_nonOwner() throws Exception {
        String body = String.format("""
                {
                    "title": "비인가 라운드",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, storeId);

        mockMvc.perform(post("/api/sessions/{sessionId}/rounds", sessionId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    @Test
    @Order(11)
    @DisplayName("10. 비소유자 라운드 수정 시 403")
    void updateRound_nonOwner() throws Exception {
        String body = String.format("""
                {
                    "title": "해킹 시도",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, storeId);

        mockMvc.perform(put("/api/rounds/{roundId}", roundId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }
}
