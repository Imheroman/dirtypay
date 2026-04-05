package com.dirtypay.integration;

import jakarta.servlet.http.Cookie;
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
 * Store 도메인 E2E 통합 테스트 — 전체 플로우.
 *
 * <p>Store Owner가 가게를 등록하고 메뉴를 관리한 후 고객 주문을 처리하는
 * 전체 비즈니스 플로우를 검증한다.</p>
 *
 * <pre>
 * [Owner] 가게 등록 -> [Owner] 메뉴 등록 -> [Customer] 주문 -> [Owner] 주문 확인/완료
 *   -> [Customer] 리뷰 작성 -> [Owner] 통계 조회
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreFullFlowIntegrationTest extends BaseIntegrationTest {

    private static String ownerToken;
    private static String customerToken;
    private static Long storeId;
    private static Long menuId;
    private static Long orderId;

    @Test
    @Order(1)
    @DisplayName("1. Owner 회원가입 + 로그인")
    void step1_ownerSignup() throws Exception {
        ownerToken = signup(
                "store-owner-flow-" + System.currentTimeMillis() + "@test.com",
                "Password1!",
                "매장 소유자"
        );
        assertThat(ownerToken).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. Customer 회원가입 + 로그인")
    void step2_customerSignup() throws Exception {
        customerToken = signup(
                "store-customer-flow-" + System.currentTimeMillis() + "@test.com",
                "Password1!",
                "고객"
        );
        assertThat(customerToken).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("3. [Owner] DIRECT 타입 가게 등록 -> 201 Created, status=ACTIVE")
    void step3_ownerCreateStore() throws Exception {
        long ts = System.currentTimeMillis();
        String body = String.format("""
                {
                    "name": "테스트 직영 매장",
                    "businessNumber": "001-%d",
                    "address": "서울시 강남구 테헤란로 1",
                    "phone": "02-1234-5678",
                    "description": "테스트 매장입니다",
                    "storeType": "DIRECT"
                }
                """, ts % 10000000);

        MvcResult result = mockMvc.perform(post("/api/stores")
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.storeType").value("DIRECT"))
                .andReturn();

        storeId = parseData(result).get("id").asLong();
        assertThat(storeId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. [Owner] 메뉴 등록 (삼겹살, 15000원) -> 201 Created, available=true")
    void step4_ownerCreateMenu() throws Exception {
        String body = """
                {
                    "name": "삼겹살",
                    "description": "국내산 삼겹살",
                    "price": 15000,
                    "category": "육류",
                    "available": true,
                    "sortOrder": 1
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("삼겹살"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andReturn();

        menuId = parseData(result).get("id").asLong();
        assertThat(menuId).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("5. [Customer] 주문 생성 (삼겹살 qty=2) -> 201 Created, status=PENDING, totalPrice=30000")
    void step5_customerCreateOrder() throws Exception {
        String body = String.format("""
                {
                    "menuId": %d,
                    "quantity": 2,
                    "customerName": "홍길동",
                    "customerPhone": "010-1234-5678"
                }
                """, menuId);

        MvcResult result = mockMvc.perform(post("/api/stores/{storeId}/orders", storeId)
                        .cookie(authCookie(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalPrice").value(30000))
                .andExpect(jsonPath("$.data.orderNumber").isNotEmpty())
                .andReturn();

        orderId = parseData(result).get("id").asLong();
        assertThat(orderId).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("6. [Owner] 주문 목록 조회 -> 1건, status=PENDING")
    void step6_ownerGetOrders() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/orders", storeId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @Order(7)
    @DisplayName("7. [Owner] 주문 CONFIRMED 상태 전이 -> 200 OK, status=CONFIRMED")
    void step7_ownerConfirmOrder() throws Exception {
        String body = """
                { "status": "CONFIRMED" }
                """;

        mockMvc.perform(patch("/api/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @Order(8)
    @DisplayName("8. [Owner] 주문 COMPLETED 상태 전이 -> 200 OK, status=COMPLETED")
    void step8_ownerCompleteOrder() throws Exception {
        String body = """
                { "status": "COMPLETED" }
                """;

        mockMvc.perform(patch("/api/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @Order(9)
    @DisplayName("9. [Customer] 리뷰 작성 (rating=5) -> 201 Created, rating=5")
    void step9_customerCreateReview() throws Exception {
        String body = """
                {
                    "rating": 5,
                    "content": "정말 맛있어요! 강추합니다."
                }
                """;

        mockMvc.perform(post("/api/stores/{storeId}/reviews", storeId)
                        .cookie(authCookie(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @Order(10)
    @DisplayName("10. [Owner] 통계 조회 -> totalOrders=1, totalRevenue=30000")
    void step10_ownerGetStatistics() throws Exception {
        mockMvc.perform(get("/api/stores/{storeId}/statistics", storeId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.storeId").value(storeId))
                .andExpect(jsonPath("$.data.totalOrders").value(1));
    }
}
