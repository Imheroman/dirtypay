package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 지갑 API 통합 테스트.
 *
 * <p><b>데이터 셋업:</b></p>
 * <ul>
 *   <li>사용자1: wallet-test@example.com / 지갑 조회, 충전, 송금 주체</li>
 *   <li>사용자2: wallet-other@example.com / 수신자 역할</li>
 * </ul>
 *
 * <p>테스트는 @Order 순서에 따라 순차 실행되며 상태를 공유한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class WalletIntegrationTest extends BaseIntegrationTest {

    // 다른 통합 테스트와 겹치지 않는 고유한 이메일 prefix 사용
    private static final String EMAIL1 = "wallet-test@example.com";
    private static final String EMAIL2 = "wallet-other@example.com";
    private static final String PASSWORD = "Password1!";

    private String token1;
    private String token2;
    private Long walletId1;

    /**
     * 테스트 실행 전 사용자2명을 회원가입하여 지갑을 자동 생성한다.
     * signup() 내부에서 AuthService.signup()이 walletService.createWallet()을 호출한다.
     */
    @BeforeAll
    void setup() throws Exception {
        this.token1 = signup(EMAIL1, PASSWORD, "지갑테스터");
        this.token2 = signup(EMAIL2, PASSWORD, "지갑타인");
    }

    // === 지갑 조회 ===

    @Test
    @Order(1)
    @DisplayName("1. 회원가입 후 지갑 자동 생성 확인 - balance=0, status=ACTIVE")
    void getMyWallet_afterSignup() throws Exception {
        // Given: 회원가입 완료된 사용자1
        // When: GET /api/wallets/me
        MvcResult result = mockMvc.perform(get("/api/wallets/me")
                        .cookie(authCookie(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        // Then: walletId를 저장하여 이후 테스트에서 재사용
        JsonNode data = parseData(result);
        this.walletId1 = data.get("id").asLong();
        assertThat(walletId1).isPositive();
    }

    // === 지갑 충전 ===

    @Test
    @Order(2)
    @DisplayName("2. 지갑 충전 성공 - 1,000,000원 충전 후 balance=1,000,000")
    void chargeWallet_success() throws Exception {
        // Given: 충전 요청 1,000,000원
        String body = """
                {"amount": 1000000}
                """;

        // When: POST /api/wallets/charge
        // Then: balance가 1,000,000원으로 업데이트됨
        mockMvc.perform(post("/api/wallets/charge")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(1000000));
    }

    @Test
    @Order(3)
    @DisplayName("3. 일일 한도 초과 충전 → 400 WALLET_005")
    void chargeWallet_dailyLimitExceeded() throws Exception {
        // Given: 이미 1,000,000원 충전 완료, 2,500,000원 추가 충전 시 합계 3,500,000 > 일일 한도 3,000,000
        String body = """
                {"amount": 2500000}
                """;

        // When: POST /api/wallets/charge
        // Then: 400 Bad Request, WALLET_005 (일일 충전 한도 초과)
        mockMvc.perform(post("/api/wallets/charge")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WALLET_005"));
    }

    @Test
    @Order(4)
    @DisplayName("4. 사용자2 지갑 충전 - 100,000원 충전 후 balance=100,000")
    void chargeWallet_user2() throws Exception {
        // When: 사용자2 100,000원 충전
        String body = """
                {"amount": 100000}
                """;

        // Then: balance=100,000
        mockMvc.perform(post("/api/wallets/charge")
                        .cookie(authCookie(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(100000));
    }

    // === 지갑 송금 ===

    @Test
    @Order(5)
    @DisplayName("5. 송금 성공 - 사용자1이 사용자2에게 50,000원 송금, type=TRANSFER_OUT")
    void transfer_success() throws Exception {
        // Given: 사용자1이 사용자2에게 50,000원 송금
        String body = """
                {"receiverEmail": "wallet-other@example.com", "amount": 50000}
                """;

        // When: POST /api/wallets/transfer
        // Then: 200, TRANSFER_OUT 타입 거래 응답
        MvcResult result = mockMvc.perform(post("/api/wallets/transfer")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("TRANSFER_OUT"))
                .andReturn();

        JsonNode txData = parseData(result);
        assertThat(txData.get("amount").asInt()).isEqualTo(50000);
        assertThat(txData.get("walletId").asLong()).isEqualTo(walletId1);
    }

    @Test
    @Order(6)
    @DisplayName("6. 잔액 부족 송금 → 400 WALLET_002")
    void transfer_insufficientBalance() throws Exception {
        // Given: 사용자1의 현재 잔액 950,000원, 99,999,999원 송금 시도
        String body = """
                {"receiverEmail": "wallet-other@example.com", "amount": 99999999}
                """;

        // When: POST /api/wallets/transfer
        // Then: 400 Bad Request, WALLET_002 (잔액 부족)
        mockMvc.perform(post("/api/wallets/transfer")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WALLET_002"));
    }

    // === 거래 이력 조회 ===

    @Test
    @Order(7)
    @DisplayName("7. 거래 이력 조회 - 충전 및 송금 이력 존재 확인")
    void getTransactions_history() throws Exception {
        // Given: walletId1에 충전(1건), 송금(1건) 거래 완료
        // When: GET /api/wallets/me/transactions
        MvcResult result = mockMvc.perform(get("/api/wallets/me/transactions")
                        .cookie(authCookie(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // Then: 거래 이력이 최소 2건 이상 존재 (충전 1건 + 송금 1건)
        JsonNode data = parseData(result);
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content").size()).isGreaterThanOrEqualTo(2);
        assertThat(data.get("totalElements").asInt()).isGreaterThanOrEqualTo(2);
    }

    // === 유효성 검증 ===

    @Test
    @Order(8)
    @DisplayName("8. 0원 이하 충전 금액 → 400 (Bean Validation)")
    void chargeWallet_invalidAmount() throws Exception {
        // Given: 충전 금액 0원 (최소 1원 이상이어야 함)
        String body = """
                {"amount": 0}
                """;

        // When/Then: 400 Bad Request
        mockMvc.perform(post("/api/wallets/charge")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("9. 자기 자신에게 송금 → 400 WALLET_007")
    void transfer_sameWallet() throws Exception {
        // Given: 사용자1의 이메일을 수신자로 지정 (자기 자신에게 송금)
        String body = """
                {"receiverEmail": "wallet-test@example.com", "amount": 1000}
                """;

        // When/Then: 400 Bad Request, WALLET_007 (같은 지갑으로 송금 불가)
        mockMvc.perform(post("/api/wallets/transfer")
                        .cookie(authCookie(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WALLET_007"));
    }

    @Test
    @Order(10)
    @DisplayName("10. 비인증 요청으로 지갑 조회 → 401")
    void getMyWallet_unauthenticated() throws Exception {
        // Given: 인증 쿠키 없이 요청
        // When/Then: 401 Unauthorized
        mockMvc.perform(get("/api/wallets/me"))
                .andExpect(status().isUnauthorized());
    }
}
