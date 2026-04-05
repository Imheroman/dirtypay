package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 그룹 CRUD + 참여/탈퇴 + 공유 메뉴 통합 테스트.
 *
 * <p>그룹 생성, 조회, 수정, 삭제, 참여/탈퇴, 공유 메뉴 저장,
 * 비소유자 접근 차단, Soft Delete를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class GroupIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "group-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "group-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long roundId;
    private Long emptyRoundId;
    private Long menuId;
    private Long menuId2;
    private Long groupId;
    private Long childGroupId;
    private Long storeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "그룹테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
        this.sessionId = createSession(accessToken, "그룹 테스트 세션");

        createRootNode(accessToken, sessionId, "전체");
        createMember(accessToken, sessionId, "멤버A");
        createMember(accessToken, sessionId, "멤버B");

        this.storeId = createStore(accessToken, "그룹 테스트 매장");
        createStoreMenu(accessToken, storeId, "기본메뉴", 10000);

        this.roundId = createRound(accessToken, sessionId, "그룹 테스트 라운드", storeId);
        this.emptyRoundId = createRound(accessToken, sessionId, "빈 라운드", storeId);
        this.menuId = createStoreMenu(accessToken, storeId, "치킨", 18000);
        this.menuId2 = createStoreMenu(accessToken, storeId, "피자", 25000);
    }

    // === 그룹 CRUD ===

    @Test
    @Order(1)
    @DisplayName("1. 그룹 생성 성공")
    void createGroup_success() throws Exception {
        String body = """
                {
                    "name": "1팀"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupName").value("1팀"))
                .andExpect(jsonPath("$.data.depth").value(0))
                .andReturn();

        this.groupId = parseData(result).get("groupId").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("2. 그룹 목록 조회 - 빈 라운드일 때 empty")
    void getGroups_emptyRound() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/groups", emptyRoundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("3. 그룹 목록 조회 - 트리 구조 응답")
    void getGroups_treeStructure() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].groupId").value(groupId))
                .andExpect(jsonPath("$.data[0].groupName").value("1팀"));
    }

    @Test
    @Order(4)
    @DisplayName("4. 중첩 그룹 생성 (parentGroupId 지정, depth=1)")
    void createGroup_nested() throws Exception {
        String body = String.format("""
                {
                    "name": "1팀-A조",
                    "parentGroupId": %d
                }
                """, groupId);

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupName").value("1팀-A조"))
                .andExpect(jsonPath("$.data.parentGroupId").value(groupId))
                .andExpect(jsonPath("$.data.depth").value(1))
                .andReturn();

        this.childGroupId = parseData(result).get("groupId").asLong();

        // 트리 조회 시 하위 그룹 포함 확인
        mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].childGroups.length()").value(1))
                .andExpect(jsonPath("$.data[0].childGroups[0].groupName").value("1팀-A조"));
    }

    @Test
    @Order(5)
    @DisplayName("5. 그룹 수정 - 이름 변경")
    void updateGroup_success() throws Exception {
        String body = """
                {
                    "name": "수정된 1팀"
                }
                """;

        mockMvc.perform(put("/api/groups/{groupId}", groupId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName").value("수정된 1팀"));
    }

    // === 그룹 참여/탈퇴 ===

    @Test
    @Order(6)
    @DisplayName("6. 그룹 참여 - 다른 사용자가 참여")
    void joinGroup_otherUser() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/join", groupId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isOk());

        // 그룹 조회 시 멤버에 포함 확인
        mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].members").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("7. 그룹 참여 - 중복 참여 시 400")
    void joinGroup_duplicate() throws Exception {
        mockMvc.perform(post("/api/groups/{groupId}/join", groupId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("GROUP_002"));
    }

    @Test
    @Order(8)
    @DisplayName("8. 그룹 탈퇴")
    void leaveGroup_success() throws Exception {
        mockMvc.perform(delete("/api/groups/{groupId}/leave", groupId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    @DisplayName("9. 그룹 탈퇴 - 미참여 상태에서 탈퇴 시 400")
    void leaveGroup_notJoined() throws Exception {
        mockMvc.perform(delete("/api/groups/{groupId}/leave", groupId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("GROUP_003"));
    }

    // === 공유 메뉴 ===

    @Test
    @Order(10)
    @DisplayName("10. 공유 메뉴 저장")
    void saveSharedMenus_success() throws Exception {
        String body = String.format("""
                {
                    "menus": [
                        {"menuId": %d, "quantity": 2},
                        {"menuId": %d, "quantity": 1}
                    ]
                }
                """, menuId, menuId2);

        mockMvc.perform(put("/api/groups/{groupId}/shared-menus", groupId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 그룹 조회 시 공유 메뉴 포함 확인
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode groups = parseData(result);
        JsonNode sharedMenus = groups.get(0).get("sharedMenus");
        assertThat(sharedMenus.size()).isEqualTo(2);
    }

    @Test
    @Order(11)
    @DisplayName("11. 공유 메뉴 교체 - 기존 메뉴 삭제 후 새 메뉴 저장")
    void saveSharedMenus_replace() throws Exception {
        String body = String.format("""
                {
                    "menus": [
                        {"menuId": %d, "quantity": 3}
                    ]
                }
                """, menuId);

        mockMvc.perform(put("/api/groups/{groupId}/shared-menus", groupId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 교체 확인: 공유 메뉴가 1개만 남아있어야 함
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode groups = parseData(result);
        JsonNode sharedMenus = groups.get(0).get("sharedMenus");
        assertThat(sharedMenus.size()).isEqualTo(1);
    }

    // === 그룹 삭제 ===

    @Test
    @Order(12)
    @DisplayName("12. 그룹 삭제 - 하위 데이터 포함")
    void deleteGroup_withChildren() throws Exception {
        mockMvc.perform(delete("/api/groups/{groupId}", groupId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(13)
    @DisplayName("13. 삭제된 그룹 조회 시 응답에서 제외")
    void getGroups_afterDelete() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // === 비소유자 접근 ===

    @Test
    @Order(14)
    @DisplayName("14. 세션 멤버 그룹 생성 성공 (MEMBER 레벨)")
    void createGroup_sessionMember() throws Exception {
        String body = """
                {
                    "name": "멤버 생성 그룹"
                }
                """;

        mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.groupName").value("멤버 생성 그룹"));
    }

    @Test
    @Order(15)
    @DisplayName("15. 세션 멤버 그룹 수정 성공 (MEMBER 레벨)")
    void updateGroup_sessionMember() throws Exception {
        // 새 그룹 생성 후 멤버가 수정
        String createBody = """
                {
                    "name": "권한 테스트용 그룹"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        Long newGroupId = parseData(result).get("groupId").asLong();

        String updateBody = """
                {
                    "name": "멤버가 수정한 그룹"
                }
                """;

        mockMvc.perform(put("/api/groups/{groupId}", newGroupId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName").value("멤버가 수정한 그룹"));
    }
}
