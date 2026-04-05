package com.dirtypay.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Store 도메인 E2E 통합 테스트 — 통계 및 인기 메뉴 조회 플로우.
 *
 * <p>여러 주문과 리뷰 후 통계 데이터의 정합성(totalOrderCount, totalRevenue)과
 * 인기 메뉴 순위의 정확성을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreStatisticsIntegrationTest extends BaseIntegrationTest {

    private static String ownerToken;
    private static String customer1Token;
    private static String customer2Token;
    private static Long storeId;
    private static Long menuAId;
    private static Long menuBId;
    private static Long order1Id;
    private static Long order2Id;
    private static Long order3Id;

    @Test
    @Order(1)
    @DisplayName("1. Owner + Customer 2명 회원가입")
    void step1_usersSignup() throws Exception {
        long ts = System.currentTimeMillis();
        ownerToken = signup("stats-owner-" + ts + "@test.com", "Password1!", "통계테스트Owner");
        customer1Token = signup("stats-c1-" + ts + "@test.com", "Password1!", "고객1");
        customer2Token = signup("stats-c2-" + ts + "@test.com", "Password1!", "고객2");
        assertThat(ownerToken).isNotNull();
        assertThat(customer1Token).isNotNull();
        assertThat(customer2Token).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. [Owner] 가게 등록 + 메뉴A(삼겹살), 메뉴B(소주) 등록")
    void step2_setupStoreAndMenus() throws Exception {
        long ts = System.currentTimeMillis();
        // 가게 등록
        String storeBody = String.format("""
                {
                    "name": "통계 테스트 매장",
                    "businessNumber": "003-%d",
                    "address": "서울시 종로구 통계로 1",
                    "storeType": "DIRECT"
                }
                """, ts % 10000000);

        MvcResult storeResult = mockMvc.perform(post("/api/stores")
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeBody))
                .andExpect(status().isCreated())
                .andReturn();
        storeId = parseData(storeResult).get("id").asLong();

        // 메뉴A 등록 (삼겹살, 15000원)
        String menuABody = """
                {
                    "name": "삼겹살",
                    "price": 15000,
                    "available": true,
                    "sortOrder": 1
                }
                """;
        MvcResult menuAResult = mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(menuABody))
                .andExpect(status().isCreated())
                .andReturn();
        menuAId = parseData(menuAResult).get("id").asLong();

        // 메뉴B 등록 (소주, 5000원)
        String menuBBody = """
                {
                    "name": "소주",
                    "price": 5000,
                    "available": true,
                    "sortOrder": 2
                }
                """;
        MvcResult menuBResult = mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(menuBBody))
                .andExpect(status().isCreated())
                .andReturn();
        menuBId = parseData(menuBResult).get("id").asLong();

        assertThat(storeId).isNotNull();
        assertThat(menuAId).isNotNull();
        assertThat(menuBId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("3. [Customer1] 메뉴A qty=2 주문 (30000원), [Customer2] 메뉴A qty=1 주문 (15000원)")
    void step3_customersOrderMenuA() throws Exception {
        String order1Body = String.format("""
                { "menuId": %d, "quantity": 2 }
                """, menuAId);
        MvcResult r1 = mockMvc.perform(post("/api/stores/{storeId}/orders", storeId)
                        .cookie(authCookie(customer1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order1Body))
                .andExpect(status().isCreated())
                .andReturn();
        order1Id = parseData(r1).get("id").asLong();

        String order2Body = String.format("""
                { "menuId": %d, "quantity": 1 }
                """, menuAId);
        MvcResult r2 = mockMvc.perform(post("/api/stores/{storeId}/orders", storeId)
                        .cookie(authCookie(customer2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order2Body))
                .andExpect(status().isCreated())
                .andReturn();
        order2Id = parseData(r2).get("id").asLong();
    }

    @Test
    @Order(4)
    @DisplayName("4. [Customer1] 메뉴B qty=1 주문 (5000원)")
    void step4_customerOrderMenuB() throws Exception {
        String order3Body = String.format("""
                { "menuId": %d, "quantity": 1 }
                """, menuBId);
        MvcResult r3 = mockMvc.perform(post("/api/stores/{storeId}/orders", storeId)
                        .cookie(authCookie(customer1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order3Body))
                .andExpect(status().isCreated())
                .andReturn();
        order3Id = parseData(r3).get("id").asLong();
    }

    @Test
    @Order(5)
    @DisplayName("5. [Owner] 모든 주문 CONFIRMED -> COMPLETED 처리")
    void step5_ownerCompleteOrders() throws Exception {
        for (Long oid : new Long[]{order1Id, order2Id, order3Id}) {
            mockMvc.perform(patch("/api/stores/{storeId}/orders/{orderId}/status", storeId, oid)
                            .cookie(authCookie(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"CONFIRMED\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/stores/{storeId}/orders/{orderId}/status", storeId, oid)
                            .cookie(authCookie(ownerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"COMPLETED\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. [Customer1, Customer2] 리뷰 작성 (5점, 3점)")
    void step6_customersWriteReviews() throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .cookie(authCookie(customer1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"content\": \"최고에요!\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .cookie(authCookie(customer2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 3, \"content\": \"보통이에요\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(7)
    @DisplayName("7. [Owner] 통계 조회 -> totalOrders=3, totalRevenue=50000 (정합성 검증)")
    void step7_ownerGetStatistics_correctlyAggregates() throws Exception {
        // 총 주문: 3건 (30000 + 15000 + 5000 = 50000)
        mockMvc.perform(get("/api/stores/{storeId}/statistics", storeId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(3))
                .andExpect(jsonPath("$.data.totalRevenue").value(50000));
    }

    @Test
    @Order(8)
    @DisplayName("8. [Owner] 인기 메뉴 조회 -> 1위 삼겹살(2건), 2위 소주(1건)")
    void step8_ownerGetPopularMenus() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/statistics/popular-menus", storeId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.menus.length()").value(2))
                .andExpect(jsonPath("$.data.menus[0].menuName").value("삼겹살"))
                .andExpect(jsonPath("$.data.menus[0].orderCount").value(2))
                .andExpect(jsonPath("$.data.menus[1].menuName").value("소주"))
                .andExpect(jsonPath("$.data.menus[1].orderCount").value(1));
    }

    @Test
    @Order(9)
    @DisplayName("9. [Customer1] 통계 조회 -> 403 Forbidden (비소유자 통계 접근 차단)")
    void step9_otherUserGetStatistics_forbidden() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/statistics", storeId)
                        .cookie(authCookie(customer1Token)))
                .andExpect(status().isForbidden());
    }
}
