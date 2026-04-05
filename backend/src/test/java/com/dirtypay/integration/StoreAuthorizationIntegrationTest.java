package com.dirtypay.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Store 도메인 E2E 통합 테스트 — 소유자 권한 검증 플로우.
 *
 * <p>비소유자의 가게 수정/삭제 시도가 403으로 차단되고,
 * 공개 GET 조회는 인증 없이도 허용되며,
 * 미인증 상태의 쓰기 작업이 401로 차단되는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreAuthorizationIntegrationTest extends BaseIntegrationTest {

    private static String ownerToken;
    private static String otherUserToken;
    private static Long storeId;

    @Test
    @Order(1)
    @DisplayName("1. Owner와 OtherUser 회원가입")
    void step1_usersSignup() throws Exception {
        long ts = System.currentTimeMillis();
        ownerToken = signup("store-auth-owner-" + ts + "@test.com", "Password1!", "매장주인");
        otherUserToken = signup("store-auth-other-" + ts + "@test.com", "Password1!", "다른사용자");
        assertThat(ownerToken).isNotNull();
        assertThat(otherUserToken).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. [Owner] 가게 등록")
    void step2_ownerCreateStore() throws Exception {
        long ts = System.currentTimeMillis();
        String body = String.format("""
                {
                    "name": "권한 테스트 매장",
                    "businessNumber": "002-%d",
                    "address": "서울시 마포구 테스트로 7",
                    "storeType": "DIRECT"
                }
                """, ts % 10000000);

        MvcResult result = mockMvc.perform(post("/api/stores")
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        storeId = parseData(result).get("id").asLong();
        assertThat(storeId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("3. [Anonymous] GET /api/stores -> 200 (인증 불필요)")
    void step3_anonymousGetStores() throws Exception {
        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(4)
    @DisplayName("4. [Anonymous] GET /api/stores/{storeId} -> 200 (인증 불필요)")
    void step4_anonymousGetStore() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storeId));
    }

    @Test
    @Order(5)
    @DisplayName("5. [Anonymous] GET /api/stores/{storeId}/menus -> 200 (인증 불필요)")
    void step5_anonymousGetMenus() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/menus", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(6)
    @DisplayName("6. [OtherUser] PUT /api/stores/{storeId} -> 403 Forbidden (비소유자 수정 차단)")
    void step6_otherUserUpdateStore_forbidden() throws Exception {
        String body = """
                {
                    "name": "악의적 수정"
                }
                """;

        mockMvc.perform(put("/api/stores/{storeId}", storeId)
                        .cookie(authCookie(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("7. [OtherUser] DELETE /api/stores/{storeId} -> 403 Forbidden (비소유자 삭제 차단)")
    void step7_otherUserDeleteStore_forbidden() throws Exception {
        mockMvc.perform(delete("/api/stores/{storeId}", storeId)
                        .cookie(authCookie(otherUserToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("8. [OtherUser] POST /api/stores/{storeId}/menus -> 403 Forbidden (비소유자 메뉴 추가 차단)")
    void step8_otherUserCreateMenu_forbidden() throws Exception {
        String body = """
                {
                    "name": "악의적 메뉴",
                    "price": 1,
                    "available": true,
                    "sortOrder": 0
                }
                """;

        mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(authCookie(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("9. [NoAuth] POST /api/stores -> 401 Unauthorized (미인증 가게 등록 차단)")
    void step9_unauthenticatedCreateStore_unauthorized() throws Exception {
        String body = """
                {
                    "name": "미인증 매장",
                    "businessNumber": "000-99-00001",
                    "address": "서울시 어딘가",
                    "storeType": "DIRECT"
                }
                """;

        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("10. [NoAuth] GET /api/stores/{storeId}/statistics -> 401 Unauthorized (미인증 통계 접근 차단)")
    void step10_unauthenticatedGetStatistics_unauthorized() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/statistics", storeId))
                .andExpect(status().isUnauthorized());
    }
}
