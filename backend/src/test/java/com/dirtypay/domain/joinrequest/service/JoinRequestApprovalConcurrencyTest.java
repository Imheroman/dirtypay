package com.dirtypay.domain.joinrequest.service;

import com.dirtypay.TestcontainersConfiguration;
import com.dirtypay.domain.joinrequest.entity.JoinRequest;
import com.dirtypay.domain.joinrequest.repository.JoinRequestRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JoinRequestService#approveJoinRequest} 동시 승인 시나리오 통합 테스트.
 *
 * <p>두 관리자가 동시에 같은 참여 요청을 승인할 때,
 * DB UNIQUE 인덱스({@code uk_session_user})가 OrgMember 중복 생성을 방지하고
 * {@link com.dirtypay.global.common.enums.ErrorCode#ALREADY_SESSION_MEMBER} 예외가 발생하는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Import(TestcontainersConfiguration.class)
class JoinRequestApprovalConcurrencyTest {

    @Autowired
    private JoinRequestService joinRequestService;

    @Autowired
    private JoinRequestRepository joinRequestRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OrgMemberRepository orgMemberRepository;

    private Long sessionId;
    private Long requestId;

    /**
     * 테스트 픽스처: 활성 세션과 PENDING 상태의 참여 요청을 각 테스트 전에 준비한다.
     */
    @BeforeEach
    void setUp() {
        // 이전 테스트 데이터 정리
        orgMemberRepository.deleteAll();
        joinRequestRepository.deleteAll();
        sessionRepository.deleteAll();

        // 활성 세션 생성
        Session session = Session.builder()
                .title("동시성 테스트 세션")
                .ownerId(999L)
                .build();
        Session savedSession = sessionRepository.save(session);
        this.sessionId = savedSession.getId();

        // PENDING 참여 요청 생성
        JoinRequest joinRequest = JoinRequest.builder()
                .sessionId(this.sessionId)
                .requesterId(100L)
                .nickname("동시테스트유저")
                .message("동시 승인 테스트")
                .build();
        JoinRequest savedRequest = joinRequestRepository.save(joinRequest);
        this.requestId = savedRequest.getId();
    }

    /**
     * 두 스레드가 동시에 같은 참여 요청을 승인할 때,
     * 정확히 1건의 OrgMember만 생성되어야 한다.
     *
     * <p>성공 1건 + 실패 1건(DataIntegrityViolationException → BusinessException)이
     * 발생하고, org_members 테이블에는 중복 없이 1건만 저장되어야 한다.</p>
     */
    @Test
    @DisplayName("두 스레드가 동시에 approve 시 OrgMember는 정확히 1건만 생성된다")
    void concurrentApprove_onlyOneOrgMemberCreated() throws InterruptedException {
        // given
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when — 두 스레드가 동시에 approveJoinRequest 호출
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();  // 동시 출발 대기
                    joinRequestService.approveJoinRequest(sessionId, requestId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // 두 스레드 동시 출발
        doneLatch.await();       // 두 스레드 모두 완료 대기
        executor.shutdown();

        // then — 성공 1건 + 실패 1건, OrgMember는 정확히 1건
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

        List<OrgMember> members = orgMemberRepository.findBySessionId(sessionId);
        assertThat(members)
                .as("동시 승인 시 OrgMember는 정확히 1건만 생성되어야 한다")
                .hasSize(1);

        OrgMember member = members.get(0);
        assertThat(member.getUserId()).isEqualTo(100L);
        assertThat(member.getNickname()).isEqualTo("동시테스트유저");
    }
}
