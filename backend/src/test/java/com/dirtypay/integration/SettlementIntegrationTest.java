package com.dirtypay.integration;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 정산 정확성 + 결제 추적 통합 테스트.
 *
 * <p><b>데이터 셋업:</b></p>
 * <ul>
 *   <li>세션 1개, 멤버 3명 (member1, member2, member3)</li>
 *   <li>Round-1: 삼겹살(15000) x2 + 소주(5000) x3 → member1,2 공유, member3 제외 → total=45,000</li>
 *   <li>Round-2: 치킨(20000) x1 → member1,2,3 전원 공유 → total=20,000</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class SettlementIntegrationTest extends BaseIntegrationTest {

    private static final String EMAIL = "settlement-test@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String OTHER_EMAIL = "settlement-other@example.com";

    private String accessToken;
    private String otherToken;
    private Long sessionId;
    private Long round1Id;
    private Long round2Id;
    private Long member1Id;
    private Long member2Id;
    private Long member3Id;
    private Long storeId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "정산테스터");
        this.otherToken = signup(OTHER_EMAIL, PASSWORD, "타인");
        this.sessionId = createSession(accessToken, "정산 테스트 세션");

        // 노드 + 멤버 3명
        createRootNode(accessToken, sessionId, "전체");
        this.member1Id = createMember(accessToken, sessionId, "홍길동");
        this.member2Id = createMember(accessToken, sessionId, "김철수");
        this.member3Id = createMember(accessToken, sessionId, "이영희");

        this.storeId = createStore(accessToken, "정산 테스트 매장");
        createStoreMenu(accessToken, storeId, "기본메뉴", 10000);

        // Round-1: 삼겹살 x2 + 소주 x3 (member1, member2 공유, member3 제외)
        this.round1Id = createRound(accessToken, sessionId, "1차 회식", storeId);

        // member3 참여자 제외
        MvcResult participantsResult = mockMvc.perform(get("/api/rounds/{roundId}/participants", round1Id)
                        .cookie(authCookie(accessToken)))
                .andReturn();
        JsonNode participants = objectMapper.readTree(participantsResult.getResponse().getContentAsString()).get("data");
        for (JsonNode p : participants) {
            if (p.get("orgMemberId").asLong() == member3Id.longValue()) {
                mockMvc.perform(put("/api/rounds/{roundId}/participants/{participantId}/exclude",
                                round1Id, p.get("id").asLong())
                                .cookie(authCookie(accessToken)))
                        .andExpect(status().isOk());
                break;
            }
        }

        Long menu1Id = createStoreMenu(accessToken, storeId, "삼겹살", 15000);
        Long menu2Id = createStoreMenu(accessToken, storeId, "소주", 5000);
        Long group1Id = createGroup(accessToken, round1Id, "1차 그룹");
        addGroupMember(group1Id, member1Id);
        addGroupMember(group1Id, member2Id);
        createOrder(accessToken, round1Id, group1Id, menu1Id, 2, member1Id, member2Id);
        createOrder(accessToken, round1Id, group1Id, menu2Id, 3, member1Id, member2Id);
        closeRound(accessToken, round1Id);

        // Round-2: 치킨 x1 (member1, member2, member3 전원 공유)
        this.round2Id = createRound(accessToken, sessionId, "2차 회식", storeId);
        Long menu3Id = createStoreMenu(accessToken, storeId, "치킨", 20000);
        Long group2Id = createGroup(accessToken, round2Id, "2차 그룹");
        addGroupMember(group2Id, member1Id);
        addGroupMember(group2Id, member2Id);
        addGroupMember(group2Id, member3Id);
        createOrder(accessToken, round2Id, group2Id, menu3Id, 1, member1Id, member2Id, member3Id);
        closeRound(accessToken, round2Id);
    }

    // === 라운드 정산 ===

    @Test
    @Order(2)
    @DisplayName("1. Round 정산 (OWNER 전략) - 정확한 금액, 분배합=원금")
    void roundSettlement_owner() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rounds/{roundId}/settlement", round1Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundId").value(round1Id))
                .andExpect(jsonPath("$.data.totalAmount").value(45000))
                .andExpect(jsonPath("$.data.settlements").isArray())
                .andReturn();

        // 분배합 = 원금 검증
        JsonNode settlements = parseData(result).get("settlements");
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode s : settlements) {
            sum = sum.add(new BigDecimal(s.get("amount").asText()));
        }
        assertThat(sum.intValue()).isEqualTo(45000);
    }

    @Test
    @Order(3)
    @DisplayName("2. Round 정산 (ROUND_UP 전략) - 전략별 차이 확인")
    void roundSettlement_roundUp() throws Exception {
        mockMvc.perform(get("/api/rounds/{roundId}/settlement", round1Id)
                        .param("strategy", "ROUND_UP")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundId").value(round1Id))
                .andExpect(jsonPath("$.data.strategy").value("ROUND_UP"))
                .andExpect(jsonPath("$.data.settlements").isArray());
    }

    // === 세션 정산 ===

    @Test
    @Order(4)
    @DisplayName("3. Session 전체 정산 - total=65,000, 멤버별 합산")
    void sessionSettlement_total() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/settlement", sessionId)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.totalAmount").value(65000))
                .andExpect(jsonPath("$.data.settlements").isArray())
                .andReturn();

        // 멤버별 합산이 total과 일치
        JsonNode settlements = parseData(result).get("settlements");
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode s : settlements) {
            sum = sum.add(new BigDecimal(s.get("amount").asText()));
        }
        assertThat(sum.intValue()).isEqualTo(65000);
    }

    @Test
    @Order(5)
    @DisplayName("4. Session 정산 - 라운드별 상세 포함")
    void sessionSettlement_withRounds() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/settlement", sessionId)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds").isArray())
                .andExpect(jsonPath("$.data.rounds.length()").value(2));
    }

    // === 멤버별 정산 ===

    @Test
    @Order(6)
    @DisplayName("5. 멤버별 정산 상세 - 라운드별 금액")
    void memberSettlement_detail() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member1Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgMemberId").value(member1Id))
                .andExpect(jsonPath("$.data.totalAmount").isNotEmpty())
                .andExpect(jsonPath("$.data.details").isArray())
                .andExpect(jsonPath("$.data.details.length()").value(2));
    }

    @Test
    @Order(7)
    @DisplayName("6. 제외 멤버 정산 - Round-1에서 0원, Round-2에서 비-0원")
    void memberSettlement_excludedMember() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member3Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgMemberId").value(member3Id))
                .andReturn();

        JsonNode details = parseData(result).get("details");
        for (JsonNode detail : details) {
            long roundId = detail.get("roundId").asLong();
            BigDecimal amount = new BigDecimal(detail.get("amount").asText());
            if (roundId == round1Id) {
                assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO);
            } else if (roundId == round2Id) {
                assertThat(amount).isGreaterThan(BigDecimal.ZERO);
            }
        }
    }

    // === 정산 완료 표시 ===

    @Test
    @Order(8)
    @DisplayName("7. 정산 완료 표시 (전액) - isPaid=true, remainingAmount=0")
    void settlementPayment_full() throws Exception {
        // member1의 총 정산 금액을 먼저 조회
        MvcResult memberResult = mockMvc.perform(get("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member1Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andReturn();

        BigDecimal totalAmount = new BigDecimal(parseData(memberResult).get("totalAmount").asText());

        // 전액 납부
        String body = String.format("""
                {
                    "paidAmount": %s
                }
                """, totalAmount.toPlainString());

        mockMvc.perform(put("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member1Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.remainingAmount").value(0));
    }

    @Test
    @Order(9)
    @DisplayName("8. 정산 부분 납부 - isPaid=false, remainingAmount > 0")
    void settlementPayment_partial() throws Exception {
        String body = """
                {
                    "paidAmount": 5000
                }
                """;

        mockMvc.perform(put("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member2Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(false))
                .andExpect(jsonPath("$.data.remainingAmount").isNotEmpty());

        // remainingAmount > 0 확인
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member2Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andReturn();

        BigDecimal remaining = new BigDecimal(parseData(result).get("remainingAmount").asText());
        assertThat(remaining).isGreaterThan(BigDecimal.ZERO);
    }

    // === 납부 금액 검증 ===

    @Test
    @Order(10)
    @DisplayName("9. 음수 납부 금액 시 400")
    void settlementPayment_negative() throws Exception {
        String body = """
                {
                    "paidAmount": -1000
                }
                """;

        mockMvc.perform(put("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member3Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").isNotEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("10. 초과 납부 금액 시 400 - SETTLEMENT_001")
    void settlementPayment_overpayment() throws Exception {
        String body = """
                {
                    "paidAmount": 9999999
                }
                """;

        mockMvc.perform(put("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member3Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_001"));
    }

    // === 빈 라운드 ===

    @Test
    @Order(12)
    @DisplayName("11. 빈 라운드 정산 - totalAmount=0")
    void emptyRoundSettlement() throws Exception {
        // 주문 없는 빈 라운드 생성
        Long emptyRoundId = createRound(accessToken, sessionId, "빈 라운드", storeId);
        closeRound(accessToken, emptyRoundId);

        mockMvc.perform(get("/api/rounds/{roundId}/settlement", emptyRoundId)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    // === 비소유자 접근 ===

    @Test
    @Order(13)
    @DisplayName("12. 비소유자 정산 완료 표시 시 403")
    void settlementPayment_nonOwner() throws Exception {
        String body = """
                {
                    "paidAmount": 1000
                }
                """;

        mockMvc.perform(put("/api/sessions/{sessionId}/settlement/members/{orgMemberId}",
                        sessionId, member1Id)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    // === 주문 중심 정산 ===

    @Test
    @Order(14)
    @DisplayName("13. 주문별 정산 조회 - 구조 검증 (orderGroups, items, members)")
    void orderSettlement_structure() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/settlement/orders", sessionId)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.strategy").value("OWNER"))
                .andExpect(jsonPath("$.data.orderGroups").isArray())
                .andExpect(jsonPath("$.data.totalAmount").isNotEmpty())
                .andReturn();

        JsonNode data = parseData(result);
        JsonNode orderGroups = data.get("orderGroups");
        assertThat(orderGroups.size()).isGreaterThan(0);

        // 각 그룹에 items가 있고, 각 item에 members가 있는지 확인
        for (JsonNode group : orderGroups) {
            assertThat(group.has("totalAmount")).isTrue();
            JsonNode items = group.get("items");
            assertThat(items.isArray()).isTrue();
            assertThat(items.size()).isGreaterThan(0);

            for (JsonNode item : items) {
                assertThat(item.has("roundId")).isTrue();
                assertThat(item.has("menuId")).isTrue();
                assertThat(item.has("menuName")).isTrue();
                assertThat(item.has("menuPrice")).isTrue();
                assertThat(item.has("quantity")).isTrue();
                assertThat(item.has("totalPrice")).isTrue();
                assertThat(item.get("members").isArray()).isTrue();
                assertThat(item.get("members").size()).isGreaterThan(0);

                for (JsonNode member : item.get("members")) {
                    assertThat(member.has("orgMemberId")).isTrue();
                    assertThat(member.has("nickname")).isTrue();
                    assertThat(member.has("shareRatio")).isTrue();
                    assertThat(member.has("totalRatio")).isTrue();
                    assertThat(member.has("amount")).isTrue();
                }
            }
        }
    }

    @Test
    @Order(15)
    @DisplayName("14. 주문별 정산 - 멤버별 shareRatio 합계가 totalRatio와 일치")
    void orderSettlement_ratioConsistency() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/settlement/orders", sessionId)
                        .param("strategy", "OWNER")
                        .cookie(authCookie(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode orderGroups = parseData(result).get("orderGroups");
        for (JsonNode group : orderGroups) {
            for (JsonNode item : group.get("items")) {
                JsonNode members = item.get("members");
                int totalRatio = members.get(0).get("totalRatio").asInt();
                int ratioSum = 0;
                for (JsonNode member : members) {
                    ratioSum += member.get("shareRatio").asInt();
                    assertThat(member.get("totalRatio").asInt()).isEqualTo(totalRatio);
                }
                assertThat(ratioSum).isEqualTo(totalRatio);
            }
        }
    }
}
