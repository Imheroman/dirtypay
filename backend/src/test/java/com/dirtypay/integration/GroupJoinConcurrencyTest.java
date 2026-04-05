package com.dirtypay.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 그룹 참여(joinGroup) 동시성 테스트.
 *
 * <p>동일 사용자가 동시에 두 번 joinGroup을 호출해도 1건만 성공하고
 * 나머지 1건은 실패(409 Conflict 또는 400 Bad Request)해야 한다.
 * DB UNIQUE 제약(uk_group_member)이 Race Condition을 최종 방어하는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class GroupJoinConcurrencyTest extends BaseIntegrationTest {

    private static final String EMAIL = "concurrency-join@example.com";
    private static final String PASSWORD = "Password1!";

    private String accessToken;
    private Long groupId;

    @BeforeAll
    void setup() throws Exception {
        this.accessToken = signup(EMAIL, PASSWORD, "동시성테스터");
        Long sessionId = createSession(accessToken, "동시성 테스트 세션");
        createRootNode(accessToken, sessionId, "전체");
        Long storeId = createStore(accessToken, "동시성 테스트 매장");
        createStoreMenu(accessToken, storeId, "메뉴1", 10000);
        Long roundId = createRound(accessToken, sessionId, "동시성 테스트 라운드", storeId);
        this.groupId = createGroup(accessToken, roundId, "동시성 그룹");
    }

    @Test
    @DisplayName("동시에 2개의 joinGroup 요청 → 정확히 1건만 성공한다")
    void joinGroup_concurrent_onlyOneSucceeds() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<MvcResult> future1 = executor.submit(() -> {
            startLatch.await();
            return mockMvc.perform(post("/api/groups/{groupId}/join", groupId)
                            .cookie(authCookie(accessToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
        });

        Future<MvcResult> future2 = executor.submit(() -> {
            startLatch.await();
            return mockMvc.perform(post("/api/groups/{groupId}/join", groupId)
                            .cookie(authCookie(accessToken))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
        });

        // 두 스레드를 동시에 출발
        startLatch.countDown();

        MvcResult result1 = future1.get();
        MvcResult result2 = future2.get();
        executor.shutdown();

        int status1 = result1.getResponse().getStatus();
        int status2 = result2.getResponse().getStatus();

        // 정확히 1건만 200 OK, 나머지 1건은 400 또는 409
        long successCount = countSuccesses(status1, status2);
        long failureCount = countFailures(status1, status2);

        assertThat(successCount)
                .as("동시 joinGroup 2건 중 성공은 정확히 1건이어야 한다")
                .isEqualTo(1);
        assertThat(failureCount)
                .as("동시 joinGroup 2건 중 실패는 정확히 1건이어야 한다")
                .isEqualTo(1);

        // DB에 실제로 1건만 저장되었는지 확인
        long memberCount = roundGroupMemberRepository.findByGroupId(groupId).size();
        assertThat(memberCount)
                .as("DB에 저장된 group_member 레코드는 정확히 1건이어야 한다")
                .isEqualTo(1);
    }

    /**
     * HTTP 상태 코드 중 200(성공) 개수를 반환한다.
     *
     * @param statuses HTTP 상태 코드 목록
     * @return 200 성공 개수
     */
    private long countSuccesses(int... statuses) {
        long count = 0;
        for (int status : statuses) {
            if (status == 200) count++;
        }
        return count;
    }

    /**
     * HTTP 상태 코드 중 실패(400 또는 409) 개수를 반환한다.
     *
     * @param statuses HTTP 상태 코드 목록
     * @return 실패 개수
     */
    private long countFailures(int... statuses) {
        long count = 0;
        for (int status : statuses) {
            if (status == 400 || status == 409) count++;
        }
        return count;
    }
}
