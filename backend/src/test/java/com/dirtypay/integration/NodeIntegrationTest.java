package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 조직도(노드) 통합 테스트.
 *
 * <p>노드 CRUD, depth 제한, 이동(순환 참조 검증), 삭제 시 하위 승격을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class NodeIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "node-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "node-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long otherSessionId;
    private Long rootNodeId;
    private Long childNodeId;
    private Long grandChildNodeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "노드테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
        this.sessionId = createSession(accessToken, "노드 테스트 세션");
        this.otherSessionId = createSession(otherToken, "타인 세션");
    }

    // === 노드 생성 ===

    @Test
    @Order(2)
    @DisplayName("1. 루트 노드 생성 - depth=0")
    void createRootNode_success() throws Exception {
        String body = """
                {
                    "name": "전체",
                    "sortOrder": 0
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("전체"))
                .andExpect(jsonPath("$.data.depth").value(0))
                .andReturn();

        this.rootNodeId = parseData(result).get("id").asLong();
    }

    @Test
    @Order(3)
    @DisplayName("2. 하위 노드 생성 - depth=1")
    void createChildNode_success() throws Exception {
        this.childNodeId = createChildNode(accessToken, sessionId, rootNodeId, "개발팀");

        mockMvc.perform(get("/api/nodes/{nodeId}", childNodeId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depth").value(1))
                .andExpect(jsonPath("$.data.parentNodeId").value(rootNodeId));
    }

    @Test
    @Order(4)
    @DisplayName("3. 트리 조회 - 트리 구조 + members[]")
    void getNodeTree() throws Exception {
        // 멤버 추가
        createMember(accessToken, sessionId, "팀장");
        createMember(accessToken, sessionId, "개발자A");

        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andReturn();

        JsonNode tree = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        // 루트 노드에 members, children이 존재
        JsonNode root = tree.get(0);
        assertThat(root.get("members")).isNotNull();
        assertThat(root.get("children")).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("4. 노드 수정 - 이름 변경")
    void updateNode_success() throws Exception {
        String body = """
                {
                    "name": "수정된 개발팀",
                    "sortOrder": 1
                }
                """;

        mockMvc.perform(put("/api/nodes/{nodeId}", childNodeId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 개발팀"));
    }

    // === depth 제한 ===

    @Test
    @Order(6)
    @DisplayName("5. depth 5 초과 시 에러 - NODE_002")
    void createNode_depthExceeded() throws Exception {
        // depth 0(루트) → depth 4 까지 총 5레벨 허용, depth 5(6번째 레벨) 시도 → NODE_002
        Long d2 = createChildNode(accessToken, sessionId, childNodeId, "depth2");
        Long d3 = createChildNode(accessToken, sessionId, d2, "depth3");
        Long d4 = createChildNode(accessToken, sessionId, d3, "depth4");

        // depth 5 시도 → 에러
        String body = String.format("""
                {
                    "parentNodeId": %d,
                    "name": "depth5-불가",
                    "sortOrder": 0
                }
                """, d4);

        mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NODE_002"));
    }

    @Test
    @Order(7)
    @DisplayName("6. 다른 세션의 부모 지정 시 에러 - NODE_003")
    void createNode_sessionMismatch() throws Exception {
        // 타인 세션에 노드 생성
        Long otherRootNodeId = createRootNode(otherToken, otherSessionId, "타인 루트");

        // 내 세션에 타인 세션의 노드를 부모로 지정
        String body = String.format("""
                {
                    "parentNodeId": %d,
                    "name": "잘못된 노드",
                    "sortOrder": 0
                }
                """, otherRootNodeId);

        mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NODE_003"));
    }

    // === 노드 이동 ===

    @Test
    @Order(8)
    @DisplayName("7. 노드 이동 성공 - 새 부모 확인")
    void moveNode_success() throws Exception {
        // 새 노드 생성 후 루트 아래로 이동
        this.grandChildNodeId = createChildNode(accessToken, sessionId, childNodeId, "이동용 노드");

        String body = String.format("""
                {
                    "targetParentNodeId": %d,
                    "sortOrder": 0
                }
                """, rootNodeId);

        mockMvc.perform(put("/api/nodes/{nodeId}/move", grandChildNodeId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentNodeId").value(rootNodeId))
                .andExpect(jsonPath("$.data.depth").value(1));
    }

    @Test
    @Order(9)
    @DisplayName("8. 자기 자신으로 이동 시 에러 - NODE_004")
    void moveNode_selfReference() throws Exception {
        String body = String.format("""
                {
                    "targetParentNodeId": %d,
                    "sortOrder": 0
                }
                """, childNodeId);

        mockMvc.perform(put("/api/nodes/{nodeId}/move", childNodeId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NODE_004"));
    }

    @Test
    @Order(10)
    @DisplayName("9. 하위 노드로 이동 시 에러 - NODE_004")
    void moveNode_circularReference() throws Exception {
        // childNodeId의 하위에 새 노드 생성
        Long subNodeId = createChildNode(accessToken, sessionId, childNodeId, "하위 노드");

        // 부모(childNodeId)를 하위(subNodeId) 아래로 이동 시도
        String body = String.format("""
                {
                    "targetParentNodeId": %d,
                    "sortOrder": 0
                }
                """, subNodeId);

        mockMvc.perform(put("/api/nodes/{nodeId}/move", childNodeId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NODE_004"));
    }

    // === 노드 삭제 시 승격 ===

    @Test
    @Order(11)
    @DisplayName("10. 노드 삭제 시 하위 노드/멤버가 부모로 이동")
    void deleteNode_childrenPromoted() throws Exception {
        // 구조: newParent → toDelete → child + member
        Long newParentId = createRootNode(accessToken, sessionId, "삭제 테스트 부모");
        Long toDeleteId = createChildNode(accessToken, sessionId, newParentId, "삭제할 노드");
        Long promotedChildId = createChildNode(accessToken, sessionId, toDeleteId, "승격될 자식");
        createMember(accessToken, sessionId, "이동될 멤버");

        // 중간 노드 삭제
        mockMvc.perform(delete("/api/nodes/{nodeId}", toDeleteId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNoContent());

        // 승격된 자식 노드의 부모가 newParent로 변경되었는지 확인
        mockMvc.perform(get("/api/nodes/{nodeId}", promotedChildId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentNodeId").value(newParentId))
                .andExpect(jsonPath("$.data.depth").value(1));
    }

    // === 비소유자 접근 ===

    @Test
    @Order(12)
    @DisplayName("11. 비소유자 노드 생성 시 403")
    void createNode_nonOwner() throws Exception {
        String body = """
                {
                    "name": "비인가 노드",
                    "sortOrder": 0
                }
                """;

        mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    @Test
    @Order(13)
    @DisplayName("12. 비소유자 노드 수정 시 403")
    void updateNode_nonOwner() throws Exception {
        String body = """
                {
                    "name": "해킹 시도",
                    "sortOrder": 0
                }
                """;

        mockMvc.perform(put("/api/nodes/{nodeId}", rootNodeId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }
}
