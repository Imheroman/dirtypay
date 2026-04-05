package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 주문 통합 테스트.
 *
 * <p>StoreMenu 기반 주문 CRUD, 가격 스냅샷 보존, CLOSED 라운드 제약,
 * 멤버별 주문 필터를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class OrderMenuIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "order-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "order-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long roundId;
    private Long groupId;
    private Long storeMenuId;
    private Long storeMenu2Id;
    private Long orderId;
    private Long member1Id;
    private Long member2Id;
    private Long storeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "주문테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
        this.sessionId = createSession(accessToken, "주문 테스트 세션");

        createRootNode(accessToken, sessionId, "전체");
        this.member1Id = createMember(accessToken, sessionId, "멤버1");
        this.member2Id = createMember(accessToken, sessionId, "멤버2");

        this.storeId = createStore(accessToken, "주문 테스트 매장");
        this.storeMenuId = createStoreMenu(accessToken, storeId, "삼겹살", 15000);
        this.storeMenu2Id = createStoreMenu(accessToken, storeId, "소주", 5000);

        this.roundId = createRound(accessToken, sessionId, "주문 라운드", storeId);
        this.groupId = createGroup(accessToken, roundId, "주문 그룹");

        addGroupMember(groupId, member1Id);
        addGroupMember(groupId, member2Id);
    }

    // === 주문 CRUD ===

    @Test
    @Order(5)
    @DisplayName("2. 주문 생성 - totalPrice = price * quantity")
    void createOrder_success() throws Exception {
        String body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": 2,
                    "memberIds": [%d, %d]
                }
                """, groupId, storeMenuId, member1Id, member2Id);

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.totalPrice").value(30000))
                .andExpect(jsonPath("$.data.menuName").value("삼겹살"))
                .andReturn();

        this.orderId = parseData(result).get("id").asLong();
    }

    @Test
    @Order(6)
    @DisplayName("3. 주문 목록 조회")
    void listOrders() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/orders", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(7)
    @DisplayName("4. 멤버별 주문 필터 - ?orgMemberId= 파라미터")
    void listOrders_filterByMember() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/orders", roundId)
                        .param("orgMemberId", member1Id.toString())
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @Order(8)
    @DisplayName("5. 주문 수정 (quantity) - totalPrice 재계산")
    void updateOrder_recalculate() throws Exception {
        String body = """
                {
                    "quantity": 3
                }
                """;

        mockMvc.perform(put("/api/orders/{orderId}", orderId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.totalPrice").value(45000));
    }

    @Test
    @Order(9)
    @DisplayName("6. StoreMenu 가격 변경 후 기존 주문의 totalPrice 불변(스냅샷 보존)")
    void storeMenuPriceChange_doesNotAffectExistingOrders() throws Exception {
        // StoreMenu 가격을 20000으로 변경
        String menuBody = """
                {
                    "name": "삼겹살",
                    "price": 20000,
                    "sortOrder": 0
                }
                """;

        mockMvc.perform(put("/api/stores/{storeId}/menus/{menuId}", storeId, storeMenuId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(menuBody))
                .andExpect(status().isOk());

        // 주문의 totalPrice가 스냅샷으로 보존되어 변경되지 않아야 한다
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/orders", roundId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode orders = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        for (JsonNode order : orders) {
            if (order.get("id").asLong() == orderId.longValue()) {
                assertThat(order.get("totalPrice").asInt())
                        .as("StoreMenu 가격 변경 후에도 기존 주문의 totalPrice는 스냅샷 값 유지")
                        .isEqualTo(45000);
            }
        }

        // 원래 가격으로 복원
        String restoreBody = """
                {
                    "name": "삼겹살",
                    "price": 15000,
                    "sortOrder": 0
                }
                """;
        mockMvc.perform(put("/api/stores/{storeId}/menus/{menuId}", storeId, storeMenuId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restoreBody))
                .andExpect(status().isOk());
    }

    // === CLOSED 라운드 제약 ===

    @Test
    @Order(10)
    @DisplayName("7. CLOSED 라운드에서 주문 생성 시 400")
    void createOrder_closedRound() throws Exception {
        closeRound(accessToken, roundId);

        String body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": 1,
                    "memberIds": [%d]
                }
                """, groupId, storeMenuId, member1Id);

        mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROUND_002"));
    }

    @Test
    @Order(11)
    @DisplayName("8. CLOSED 라운드에서 주문 수정 시 400")
    void updateOrder_closedRound() throws Exception {
        String body = """
                {
                    "quantity": 5
                }
                """;

        mockMvc.perform(put("/api/orders/{orderId}", orderId)
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROUND_002"));
    }

    @Test
    @Order(12)
    @DisplayName("9. CLOSED 라운드에서 주문 삭제 시 400")
    void deleteOrder_closedRound() throws Exception {
        mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROUND_002"));
    }

    // === 주문 삭제 (OPEN) ===

    @Test
    @Order(13)
    @DisplayName("10. 주문 삭제 (OPEN 라운드) - soft delete")
    void deleteOrder_openRound() throws Exception {
        reopenRound(accessToken, roundId);

        Long tempOrderId = createOrder(accessToken, roundId, storeMenu2Id, 1, member1Id);

        mockMvc.perform(delete("/api/orders/{orderId}", tempOrderId)
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isNoContent());
    }

    // === 비소유자 접근 ===

    @Test
    @Order(14)
    @DisplayName("11. 비소유자 주문 생성 시 403")
    void createOrder_nonOwner() throws Exception {
        String body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": 1,
                    "memberIds": [%d]
                }
                """, groupId, storeMenuId, member1Id);

        mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }
}
