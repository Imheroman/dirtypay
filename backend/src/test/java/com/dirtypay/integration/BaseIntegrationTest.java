package com.dirtypay.integration;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통 베이스 클래스.
 *
 * <p>회원가입, 로그인, 세션/노드/멤버/라운드/메뉴/주문 생성 등
 * 반복적으로 사용하는 헬퍼 메서드를 제공한다.</p>
 *
 * <p>TestContainers를 통해 실제 MariaDB 10.11 컨테이너를 사용하여
 * H2와의 방언 불일치를 방지하고 프로덕션 환경과 동일한 조건에서 테스트한다.</p>
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
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RoundGroupMemberRepository roundGroupMemberRepository;

    /**
     * 회원가입 후 access_token을 반환한다.
     */
    protected String signup(String email, String password, String name) throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, email, password, name);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        return login(email, password);
    }

    /**
     * 로그인 후 access_token 쿠키 값을 반환한다.
     */
    protected String login(String email, String password) throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("access_token");
        return cookie != null ? cookie.getValue() : null;
    }

    /**
     * 로그인 후 refresh_token을 반환한다.
     */
    protected String loginAndGetRefreshToken(String email, String password) throws Exception {
        String body = String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = parseData(result);
        return data.get("refreshToken").asText();
    }

    /**
     * 세션을 생성하고 ID를 반환한다.
     */
    protected Long createSession(String token, String title) throws Exception {
        String body = String.format("""
                {
                    "title": "%s",
                    "description": "테스트 세션",
                    "startDate": "2026-01-01",
                    "endDate": "2026-12-31"
                }
                """, title);

        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 루트 노드를 생성하고 ID를 반환한다.
     */
    protected Long createRootNode(String token, Long sessionId, String name) throws Exception {
        String body = String.format("""
                {
                    "name": "%s",
                    "sortOrder": 0
                }
                """, name);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 하위 노드를 생성하고 ID를 반환한다.
     */
    protected Long createChildNode(String token, Long sessionId, Long parentNodeId, String name) throws Exception {
        String body = String.format("""
                {
                    "parentNodeId": %d,
                    "name": "%s",
                    "sortOrder": 0
                }
                """, parentNodeId, name);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/nodes", sessionId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 조직 멤버를 생성하고 ID를 반환한다.
     */
    protected Long createMember(String token, Long sessionId, String nickname) throws Exception {
        String body = String.format("""
                {
                    "nickname": "%s"
                }
                """, nickname);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/members", sessionId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 매장을 생성하고 ID를 반환한다.
     */
    protected Long createStore(String token, String name) throws Exception {
        long ts = System.currentTimeMillis();
        String body = String.format("""
                {
                    "name": "%s",
                    "businessNumber": "999-%d",
                    "address": "테스트 주소",
                    "storeType": "DIRECT"
                }
                """, name, ts % 10000000);

        MvcResult result = mockMvc.perform(post("/api/stores")
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 매장 메뉴를 생성하고 ID를 반환한다.
     */
    protected Long createStoreMenu(String token, Long storeId, String name, int price) throws Exception {
        String body = String.format("""
                {
                    "name": "%s",
                    "price": %d,
                    "available": true,
                    "sortOrder": 0
                }
                """, name, price);

        MvcResult result = mockMvc.perform(post("/api/stores/{storeId}/menus", storeId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 라운드를 생성하고 ID를 반환한다.
     */
    protected Long createRound(String token, Long sessionId, String title, Long storeId) throws Exception {
        String body = String.format("""
                {
                    "title": "%s",
                    "place": "테스트 장소",
                    "roundDate": "2026-03-01",
                    "sortOrder": 0,
                    "storeId": %d
                }
                """, title, storeId);

        MvcResult result = mockMvc.perform(post("/api/sessions/{sessionId}/rounds", sessionId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 그룹을 생성하고 ID를 반환한다.
     */
    protected Long createGroup(String token, Long roundId, String name) throws Exception {
        String body = String.format("""
                {
                    "name": "%s"
                }
                """, name);

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/groups", roundId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("groupId").asLong();
    }

    /**
     * 그룹에 멤버를 추가한다.
     */
    protected void addGroupMember(Long groupId, Long orgMemberId) {
        roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(groupId)
                .orgMemberId(orgMemberId)
                .build());
    }

    /**
     * 주문을 생성하고 ID를 반환한다.
     */
    protected Long createOrder(String token, Long roundId, Long groupId, Long menuId, int quantity, Long... memberIds) throws Exception {
        StringBuilder memberIdsStr = new StringBuilder();
        for (int i = 0; i < memberIds.length; i++) {
            if (i > 0) memberIdsStr.append(", ");
            memberIdsStr.append(memberIds[i]);
        }

        String body = String.format("""
                {
                    "groupId": %d,
                    "menuId": %d,
                    "quantity": %d,
                    "memberIds": [%s]
                }
                """, groupId, menuId, quantity, memberIdsStr);

        MvcResult result = mockMvc.perform(post("/api/rounds/{roundId}/orders", roundId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return parseData(result).get("id").asLong();
    }

    /**
     * 주문을 생성하고 ID를 반환한다. (groupId 없이 - 하위호환용)
     *
     * @deprecated 그룹 기반 주문으로 전환. {@link #createOrder(String, Long, Long, Long, int, Long...)} 사용 권장
     */
    @Deprecated
    protected Long createOrder(String token, Long roundId, Long menuId, int quantity, Long... memberIds) throws Exception {
        Long groupId = createGroup(token, roundId, "기본 그룹");
        for (Long memberId : memberIds) {
            addGroupMember(groupId, memberId);
        }
        return createOrder(token, roundId, groupId, menuId, quantity, memberIds);
    }

    /**
     * 라운드를 CLOSED 상태로 변경한다.
     */
    protected void closeRound(String token, Long roundId) throws Exception {
        String body = """
                {
                    "status": "CLOSED"
                }
                """;

        mockMvc.perform(put("/api/rounds/{roundId}/status", roundId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    /**
     * 라운드를 OPEN 상태로 변경한다.
     */
    protected void reopenRound(String token, Long roundId) throws Exception {
        String body = """
                {
                    "status": "OPEN"
                }
                """;

        mockMvc.perform(put("/api/rounds/{roundId}/status", roundId)
                        .cookie(new Cookie("access_token", token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    /**
     * MvcResult에서 $.data 노드를 파싱하여 반환한다.
     */
    protected JsonNode parseData(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return objectMapper.readTree(content).get("data");
    }

    /**
     * MvcResult에서 $.error 노드를 파싱하여 반환한다.
     */
    protected JsonNode parseError(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return objectMapper.readTree(content).get("error");
    }

    /**
     * 인증된 요청을 위한 Cookie를 생성한다.
     */
    protected Cookie authCookie(String token) {
        return new Cookie("access_token", token);
    }
}
