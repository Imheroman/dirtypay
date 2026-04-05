package com.dirtypay.integration;

import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.dirtypay.TestcontainersConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Session → 조직도 → Round → 주문 → 정산 전체 플로우 E2E 통합 테스트.
 *
 * <p>실제 HTTP 요청 → 인증 → 비즈니스 로직 → DB 저장 → 조회까지 전체 흐름을 검증한다.
 * {@code @Transactional}을 사용하지 않아 실제 커밋 후 데이터 흐름을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Import(TestcontainersConfiguration.class)
class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoundGroupMemberRepository roundGroupMemberRepository;

    // 테스트 간 공유 상태
    private String accessToken;
    private Long sessionId;
    private Long rootNodeId;
    private Long childNodeId;
    private Long member1Id;
    private Long member2Id;
    private Long member3Id;
    private Long roundId;
    private Long groupId;
    private Long menu1Id;
    private Long menu2Id;
    private Long participantIdToExclude;
    private Long storeId;

    // === 1. 인증 ===

    @Test
    @Order(1)
    @DisplayName("1. 회원가입 - POST /api/auth/signup")
    void signup() throws Exception {
        String body = """
                {
                    "email": "test@example.com",
                    "password": "Password1!",
                    "name": "테스터"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("테스터"));
    }

    @Test
    @Order(2)
    @DisplayName("2. 로그인 - POST /api/auth/login → access_token 쿠키 획득")
    void login() throws Exception {
        String body = """
                {
                    "email": "test@example.com",
                    "password": "Password1!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("access_token");
        Assertions.assertNotNull(cookie, "access_token 쿠키가 존재해야 한다");
        this.accessToken = cookie.getValue();
    }

    // === 2. 세션 ===

    @Test
    @Order(3)
    @DisplayName("3. 세션 생성 - POST /api/sessions")
    void createSession() throws Exception {
        String body = """
                {
                    "title": "E2E 테스트 정산",
                    "description": "통합 테스트용 세션",
                    "startDate": "2026-01-01",
                    "endDate": "2026-12-31"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("E2E 테스트 정산"))
                .andReturn();

        JsonNode data = parseData(result);
        this.sessionId = data.get("id").asLong();
    }

    @Test
    @Order(4)
    @DisplayName("4. 세션 조회 - GET /api/sessions/{id}")
    void getSession() throws Exception {
        mockMvc.perform(get("/api/sessions/{id}", sessionId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sessionId))
                .andExpect(jsonPath("$.data.title").value("E2E 테스트 정산"));
    }

    // === 3. 조직도 ===

    @Test
    @Order(5)
    @DisplayName("5. 루트 노드 생성 - POST /api/sessions/{id}/nodes")
    void createRootNode() throws Exception {
        String body = """
                {
                    "name": "전체",
                    "sortOrder": 0
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("전체"))
                .andExpect(jsonPath("$.data.depth").value(0))
                .andReturn();

        JsonNode data = parseData(result);
        this.rootNodeId = data.get("id").asLong();
    }

    @Test
    @Order(6)
    @DisplayName("6. 하위 노드 생성 - POST /api/sessions/{id}/nodes (parentId 지정)")
    void createChildNode() throws Exception {
        String body = String.format("""
                {
                    "parentNodeId": %d,
                    "name": "개발팀",
                    "sortOrder": 0
                }
                """, rootNodeId);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("개발팀"))
                .andExpect(jsonPath("$.data.depth").value(1))
                .andReturn();

        JsonNode data = parseData(result);
        this.childNodeId = data.get("id").asLong();
    }

    @Test
    @Order(7)
    @DisplayName("7. 조직 멤버 추가 - POST /api/sessions/{id}/members (3명)")
    void createMembers() throws Exception {
        this.member1Id = createMember(sessionId, "홍길동");
        this.member2Id = createMember(sessionId, "김철수");
        this.member3Id = createMember(sessionId, "이영희");
    }

    @Test
    @Order(8)
    @DisplayName("8. 조직도 트리 조회 - GET /api/sessions/{id}/nodes")
    void getNodeTree() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    // === 4. 라운드 ===

    @Test
    @Order(9)
    @DisplayName("9. 라운드 생성 - POST /api/sessions/{id}/rounds → 참여자 자동 초기화")
    void createRound() throws Exception {
        // 가게 + 메뉴 생성 (storeId 필수)
        String storeBody = """
                {
                    "name": "테스트 매장",
                    "businessNumber": "999-1234567",
                    "address": "테스트 주소",
                    "storeType": "DIRECT"
                }
                """;
        MvcResult storeResult = mockMvc.perform(post("/api/stores")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeBody))
                .andExpect(status().isCreated())
                .andReturn();
        this.storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        String menuBody = """
                {
                    "name": "기본메뉴",
                    "price": 10000,
                    "available": true,
                    "sortOrder": 0
                }
                """;
        mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(menuBody))
                .andExpect(status().isCreated());

        String body = String.format("""
                {
                    "title": "1차 회식",
                    "place": "강남역 고깃집",
                    "roundDate": "2026-03-01",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, storeId);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/rounds", sessionId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("1차 회식"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        JsonNode data = parseData(result);
        this.roundId = data.get("id").asLong();
    }

    @Test
    @Order(10)
    @DisplayName("10. 참여자 조회 - GET /api/rounds/{id}/participants")
    void getParticipants() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/participants", roundId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andReturn();

        // 제외할 참여자 ID 저장 (마지막 참여자)
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data");
        this.participantIdToExclude = data.get(3).get("id").asLong();
    }

    @Test
    @Order(11)
    @DisplayName("11. 참여자 제외 - PUT /api/rounds/{id}/participants/{id}/exclude")
    void excludeParticipant() throws Exception {
        mockMvc.perform(put("/api/rounds/{roundId}/participants/{participantId}/exclude",
                        roundId, participantIdToExclude)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.excluded").value(true));
    }

    // === 5. 메뉴 ===

    @Test
    @Order(12)
    @DisplayName("12. 메뉴 등록 - POST /api/rounds/{id}/menus (2개)")
    void createMenus() throws Exception {
        this.menu1Id = createStoreMenu("삼겹살", 15000);
        this.menu2Id = createStoreMenu("소주", 5000);
    }

    @Test
    @Order(13)
    @DisplayName("13. 가게 메뉴 목록 조회 - GET /api/stores/{id}/menus/available")
    void getMenus() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/menus/available", storeId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // === 6. 주문 ===

    @Test
    @Order(14)
    @DisplayName("14. 주문 생성 - POST /api/rounds/{id}/orders (그룹 기반 멤버별 주문)")
    void createOrders() throws Exception {
        // 그룹 생성
        String groupBody = """
                {
                    "name": "메인 그룹"
                }
                """;
        MvcResult groupResult = mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupBody))
                .andExpect(status().isCreated())
                .andReturn();
        this.groupId = parseData(groupResult).get("groupId").asLong();

        roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(groupId).orgMemberId(member1Id).build());
        roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(groupId).orgMemberId(member2Id).build());

        // 삼겹살 2인분 - 멤버 1, 2가 공유
        String order1Body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": 2,
                    "memberIds": [%d, %d]
                }
                """, groupId, menu1Id, member1Id, member2Id);

        mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order1Body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(2));

        // 소주 3병 - 멤버 1, 2가 공유
        String order2Body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": 3,
                    "memberIds": [%d, %d]
                }
                """, groupId, menu2Id, member1Id, member2Id);

        mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order2Body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quantity").value(3));
    }

    @Test
    @Order(15)
    @DisplayName("15. 주문 목록 조회 - GET /api/rounds/{id}/orders")
    void getOrders() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/orders", roundId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // === 7. 라운드 마감 ===

    @Test
    @Order(16)
    @DisplayName("16. 라운드 상태 변경(CLOSED) - PUT /api/rounds/{id}/status")
    void closeRound() throws Exception {
        String body = """
                {
                    "status": "CLOSED"
                }
                """;

        mockMvc.perform(put("/api/rounds/{roundId}/status", roundId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    // === 8. 정산 ===

    @Test
    @Order(17)
    @DisplayName("17. 라운드 정산 조회 - GET /api/rounds/{id}/settlement?strategy=EQUAL")
    void getRoundSettlement() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/settlement", roundId)
                        .param("strategy", "OWNER")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roundId").value(roundId))
                .andExpect(jsonPath("$.data.totalAmount").isNotEmpty())
                .andExpect(jsonPath("$.data.settlements").isArray());
    }

    @Test
    @Order(18)
    @DisplayName("18. 세션 전체 정산 조회 - GET /api/sessions/{id}/settlement?strategy=OWNER")
    void getSessionSettlement() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/settlement", sessionId)
                        .param("strategy", "OWNER")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.totalAmount").isNotEmpty())
                .andExpect(jsonPath("$.data.settlements").isArray());
    }

    @Test
    @Order(19)
    @DisplayName("19. 멤버별 정산 조회 - GET /api/sessions/{id}/settlement/members/{orgMemberId}")
    void getMemberSettlement() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member1Id)
                        .param("strategy", "OWNER")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgMemberId").value(member1Id))
                .andExpect(jsonPath("$.data.totalAmount").isNotEmpty());
    }

    // === 헬퍼 메서드 ===

    private Long createMember(Long sessionId, String nickname) throws Exception {
        String body = String.format("""
                {
                    "nickname": "%s"
                }
                """, nickname);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/members", sessionId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    private Long createStoreMenu(String name, int price) throws Exception {
        String body = String.format("""
                {
                    "name": "%s",
                    "price": %d,
                    "available": true,
                    "sortOrder": 0
                }
                """, name, price);

        MvcResult result = mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    private JsonNode parseData(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return objectMapper.readTree(content).get("data");
    }
}
