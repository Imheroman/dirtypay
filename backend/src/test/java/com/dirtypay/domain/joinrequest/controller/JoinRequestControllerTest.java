package com.dirtypay.domain.joinrequest.controller;

import com.dirtypay.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link JoinRequestController} MockMvc 통합 테스트.
 *
 * <p>참여 요청 생성, 목록 조회, 승인, 거절 엔드포인트와
 * 인증/인가 시나리오, 요청 검증(@Valid) 동작을 검증한다.</p>
 *
 * <p>TestContainers를 통해 실제 MariaDB 10.11 환경에서 실행한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class JoinRequestControllerTest extends BaseIntegrationTest {

    // 세션 소유자 (초대 요청을 승인/거절하는 쪽)
    private static final String OWNER_EMAIL    = "jr-owner@example.com";
    private static final String OWNER_PASSWORD = "Password1!";

    // 참여 요청자 (초대 코드로 세션에 참여 신청하는 쪽)
    private static final String USER_EMAIL     = "jr-user@example.com";
    private static final String USER_PASSWORD  = "Password1!";

    // 관련 없는 제3자 (세션 미참여)
    private static final String OTHER_EMAIL    = "jr-other@example.com";
    private static final String OTHER_PASSWORD = "Password1!";

    private String ownerToken;
    private String userToken;
    private String otherToken;

    /** 세션 ID (소유자가 생성) */
    private Long sessionId;

    /** 세션 초대 코드 */
    private String inviteCode;

    /** 생성된 참여 요청 ID */
    private Long joinRequestId;

    /**
     * 테스트 픽스처를 준비한다.
     *
     * <p>소유자·사용자·제3자 계정을 생성하고,
     * 소유자가 세션을 만들어 초대 코드를 추출한다.</p>
     */
    @BeforeAll
    void setup() throws Exception {
        this.ownerToken = signup(OWNER_EMAIL, OWNER_PASSWORD, "세션소유자");
        this.userToken  = signup(USER_EMAIL,  USER_PASSWORD,  "참여신청자");
        this.otherToken = signup(OTHER_EMAIL, OTHER_PASSWORD, "제3자");

        // 세션 생성
        String sessionBody = """
                {
                    "title": "JoinRequest 테스트 세션",
                    "description": "통합 테스트용",
                    "startDate": "2026-01-01",
                    "endDate": "2026-12-31"
                }
                """;

        MvcResult sessionResult = mockMvc.perform(post("/api/sessions")
                        .cookie(authCookie(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode sessionData = parseData(sessionResult);
        this.sessionId  = sessionData.get("id").asLong();
        this.inviteCode = sessionData.get("inviteCode").asText();
    }

    // =========================================================================
    // 1. 참여 요청 생성
    // =========================================================================

    /**
     * 유효한 초대 코드로 참여 요청을 제출하면 201 Created와 PENDING 상태 응답이 반환된다.
     */
    @Test
    @Order(10)
    @DisplayName("참여 요청 생성 성공 - 유효한 초대 코드와 닉네임")
    void createJoinRequest_success() throws Exception {
        String body = """
                {
                    "nickname": "신청자닉네임",
                    "message": "함께하고 싶습니다"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .cookie(authCookie(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.nickname").value("신청자닉네임"))
                .andExpect(jsonPath("$.data.message").value("함께하고 싶습니다"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        this.joinRequestId = parseData(result).get("id").asLong();
        assertThat(this.joinRequestId).isPositive();
    }

    /**
     * 이미 PENDING 상태의 요청이 있을 때 같은 세션에 재요청하면 409 Conflict가 반환된다.
     */
    @Test
    @Order(11)
    @DisplayName("참여 요청 생성 실패 - 중복 PENDING 요청 시 409")
    void createJoinRequest_duplicatePending_conflict() throws Exception {
        String body = """
                {
                    "nickname": "중복신청",
                    "message": "또 신청"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .cookie(authCookie(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JOIN_002"));
    }

    /**
     * 존재하지 않는 초대 코드로 요청하면 404 Not Found가 반환된다.
     */
    @Test
    @Order(12)
    @DisplayName("참여 요청 생성 실패 - 존재하지 않는 초대 코드 시 404")
    void createJoinRequest_invalidInviteCode_notFound() throws Exception {
        String body = """
                {
                    "nickname": "잘못된코드신청"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", "INVALID99")
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_001"));
    }

    /**
     * 인증 없이 참여 요청을 제출하면 401 Unauthorized가 반환된다.
     */
    @Test
    @Order(13)
    @DisplayName("참여 요청 생성 실패 - 미인증 접근 시 401")
    void createJoinRequest_noAuth_unauthorized() throws Exception {
        String body = """
                {
                    "nickname": "미인증신청"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. 요청 검증 (@Valid)
    // =========================================================================

    /**
     * nickname이 공백이면 @Valid 검증에 실패하여 400 Bad Request가 반환된다.
     */
    @Test
    @Order(20)
    @DisplayName("참여 요청 생성 실패 - nickname 공백 시 400")
    void createJoinRequest_blankNickname_badRequest() throws Exception {
        String body = """
                {
                    "nickname": "",
                    "message": "닉네임 없음"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * nickname 필드 자체가 없으면 @Valid 검증에 실패하여 400 Bad Request가 반환된다.
     */
    @Test
    @Order(21)
    @DisplayName("참여 요청 생성 실패 - nickname 누락 시 400")
    void createJoinRequest_missingNickname_badRequest() throws Exception {
        String body = """
                {
                    "message": "닉네임 없음"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .cookie(authCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. 참여 요청 목록 조회
    // =========================================================================

    /**
     * 세션 소유자가 전체 참여 요청 목록을 조회하면 200 OK와 페이지 응답이 반환된다.
     */
    @Test
    @Order(30)
    @DisplayName("참여 요청 목록 조회 성공 - 소유자가 전체 조회")
    void getJoinRequests_owner_success() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/join-requests", sessionId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    /**
     * status 쿼리 파라미터로 PENDING 상태만 필터링하면 해당 상태의 요청만 반환된다.
     */
    @Test
    @Order(31)
    @DisplayName("참여 요청 목록 조회 - status=PENDING 필터링")
    void getJoinRequests_filterByStatus_pending() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/{sessionId}/join-requests", sessionId)
                        .param("status", "PENDING")
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        // PENDING 요청이 1건 이상 포함되어야 한다 (Order 10에서 생성한 요청)
        JsonNode data = parseData(result);
        JsonNode content = data.get("content");
        assertThat(content.size()).isGreaterThanOrEqualTo(1);

        for (JsonNode item : content) {
            assertThat(item.get("status").asText())
                    .as("status 필터 결과는 모두 PENDING이어야 한다")
                    .isEqualTo("PENDING");
        }
    }

    /**
     * 세션에 참여하지 않은 제3자가 참여 요청 목록을 조회하면 403 Forbidden이 반환된다.
     */
    @Test
    @Order(32)
    @DisplayName("참여 요청 목록 조회 실패 - 비소유자(제3자) 접근 시 403")
    void getJoinRequests_nonOwner_forbidden() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/join-requests", sessionId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    /**
     * 인증 없이 목록을 조회하면 401 Unauthorized가 반환된다.
     */
    @Test
    @Order(33)
    @DisplayName("참여 요청 목록 조회 실패 - 미인증 접근 시 401")
    void getJoinRequests_noAuth_unauthorized() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}/join-requests", sessionId))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 4. 참여 요청 승인
    // =========================================================================

    /**
     * 세션 소유자가 PENDING 상태의 참여 요청을 승인하면 200 OK와 APPROVED 상태 응답이 반환된다.
     */
    @Test
    @Order(40)
    @DisplayName("참여 요청 승인 성공 - 소유자가 PENDING 요청 승인")
    void approveJoinRequest_success() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/approve",
                        sessionId, joinRequestId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(joinRequestId))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    /**
     * 이미 APPROVED 상태인 요청을 다시 승인하면 400 Bad Request가 반환된다.
     */
    @Test
    @Order(41)
    @DisplayName("참여 요청 승인 실패 - 이미 승인된 요청 재승인 시 400")
    void approveJoinRequest_alreadyApproved_badRequest() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/approve",
                        sessionId, joinRequestId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("JOIN_003"));
    }

    /**
     * 세션에 참여하지 않은 제3자가 참여 요청을 승인하려 하면 403 Forbidden이 반환된다.
     */
    @Test
    @Order(42)
    @DisplayName("참여 요청 승인 실패 - 비소유자(제3자) 접근 시 403")
    void approveJoinRequest_nonOwner_forbidden() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/approve",
                        sessionId, joinRequestId)
                        .cookie(authCookie(otherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    /**
     * 인증 없이 승인 요청을 하면 401 Unauthorized가 반환된다.
     */
    @Test
    @Order(43)
    @DisplayName("참여 요청 승인 실패 - 미인증 접근 시 401")
    void approveJoinRequest_noAuth_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/approve",
                        sessionId, joinRequestId))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 존재하지 않는 참여 요청 ID로 승인 시 404 Not Found가 반환된다.
     */
    @Test
    @Order(44)
    @DisplayName("참여 요청 승인 실패 - 존재하지 않는 요청 ID 시 404")
    void approveJoinRequest_notFound() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/approve",
                        sessionId, 999999L)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOIN_001"));
    }

    // =========================================================================
    // 5. 참여 요청 거절
    // =========================================================================

    /**
     * 거절 테스트를 위해 별도의 참여 요청을 생성하고 거절한다.
     *
     * <p>Order 40에서 기존 요청(joinRequestId)이 이미 승인된 상태이므로
     * 제3자 계정으로 새 참여 요청을 생성한 뒤 거절한다.</p>
     */
    @Test
    @Order(50)
    @DisplayName("참여 요청 거절 성공 - 소유자가 PENDING 요청 거절")
    void rejectJoinRequest_success() throws Exception {
        // 제3자가 새 참여 요청 생성
        String body = """
                {
                    "nickname": "거절대상자",
                    "message": "거절 테스트"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                                .cookie(authCookie(otherToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Long rejectTargetId = parseData(createResult).get("id").asLong();

        // 소유자가 거절
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, rejectTargetId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(rejectTargetId))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    /**
     * 이미 REJECTED 상태인 요청을 다시 거절하면 400 Bad Request가 반환된다.
     */
    @Test
    @Order(51)
    @DisplayName("참여 요청 거절 실패 - 이미 거절된 요청 재거절 시 400")
    void rejectJoinRequest_alreadyRejected_badRequest() throws Exception {
        // Order 50에서 생성한 새 거절 요청을 재사용하기 위해 새 요청을 한 번 더 생성 후 거절
        // 별도 사용자(userToken)로 재요청 (userToken은 Order 40에서 이미 APPROVED됨)
        // -> 이미 승인된 user를 다시 재요청하면 JOIN_004(이미 멤버)가 반환됨
        // 따라서, 이번 Order 51에서는 Order 50의 rejectTargetId가 없으므로
        // 새 임시 요청을 또 생성하는 대신, 존재하는 REJECTED 상태 요청을 재거절하는 시나리오를
        // 독립적으로 구성한다.

        // 임시 계정으로 새 참여 요청 생성
        String tmpEmail    = "jr-reject2@example.com";
        String tmpPassword = "Password1!";
        String tmpToken    = signup(tmpEmail, tmpPassword, "재거절테스트");

        String reqBody = """
                {
                    "nickname": "재거절대상자"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                                .cookie(authCookie(tmpToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reqBody))
                .andExpect(status().isCreated())
                .andReturn();

        Long tmpRequestId = parseData(createResult).get("id").asLong();

        // 1차 거절
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, tmpRequestId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isOk());

        // 2차 재거절 → 400
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, tmpRequestId)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("JOIN_003"));
    }

    /**
     * 세션에 참여하지 않은 제3자가 참여 요청을 거절하려 하면 403 Forbidden이 반환된다.
     *
     * <p>새 임시 계정으로 PENDING 요청을 만든 뒤, 다른 제3자가 거절을 시도한다.</p>
     */
    @Test
    @Order(52)
    @DisplayName("참여 요청 거절 실패 - 비소유자 접근 시 403")
    void rejectJoinRequest_nonOwner_forbidden() throws Exception {
        // 임시 계정으로 PENDING 참여 요청 생성
        String tmpEmail    = "jr-reject3@example.com";
        String tmpPassword = "Password1!";
        String tmpToken    = signup(tmpEmail, tmpPassword, "거절권한테스트");

        String reqBody = """
                {
                    "nickname": "거절권한테스트자"
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                                .cookie(authCookie(tmpToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reqBody))
                .andExpect(status().isCreated())
                .andReturn();

        Long pendingRequestId = parseData(createResult).get("id").asLong();

        // 제3자(tmpToken 자신)가 거절 시도 → 403
        // tmpToken 유저는 세션 소유자도 아니고 OrgMember도 아니므로 SESSION_ACCESS_DENIED
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, pendingRequestId)
                        .cookie(authCookie(tmpToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_002"));
    }

    /**
     * 인증 없이 거절 요청을 하면 401 Unauthorized가 반환된다.
     */
    @Test
    @Order(53)
    @DisplayName("참여 요청 거절 실패 - 미인증 접근 시 401")
    void rejectJoinRequest_noAuth_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, joinRequestId))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 존재하지 않는 참여 요청 ID로 거절 시 404 Not Found가 반환된다.
     */
    @Test
    @Order(54)
    @DisplayName("참여 요청 거절 실패 - 존재하지 않는 요청 ID 시 404")
    void rejectJoinRequest_notFound() throws Exception {
        mockMvc.perform(patch("/api/sessions/{sessionId}/join-requests/{requestId}/reject",
                        sessionId, 999999L)
                        .cookie(authCookie(ownerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOIN_001"));
    }

    // =========================================================================
    // 6. 이미 세션 멤버인 경우 참여 요청 시도
    // =========================================================================

    /**
     * 이미 세션에 참여한 멤버(APPROVED 처리된 사용자)가 다시 참여 요청을 하면 409 Conflict가 반환된다.
     */
    @Test
    @Order(60)
    @DisplayName("참여 요청 생성 실패 - 이미 세션 멤버인 경우 409")
    void createJoinRequest_alreadyMember_conflict() throws Exception {
        // userToken 소유자는 Order 40에서 이미 APPROVED됨 → OrgMember 존재
        String body = """
                {
                    "nickname": "재신청시도"
                }
                """;

        mockMvc.perform(post("/api/sessions/invite/{inviteCode}/join-requests", inviteCode)
                        .cookie(authCookie(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("JOIN_004"));
    }
}
