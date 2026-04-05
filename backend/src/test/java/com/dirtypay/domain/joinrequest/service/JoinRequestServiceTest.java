package com.dirtypay.domain.joinrequest.service;

import com.dirtypay.domain.joinrequest.dto.request.JoinRequestCreateRequest;
import com.dirtypay.domain.joinrequest.dto.response.JoinRequestResponse;
import com.dirtypay.domain.joinrequest.entity.JoinRequest;
import com.dirtypay.domain.joinrequest.entity.JoinRequestStatus;
import com.dirtypay.domain.joinrequest.repository.JoinRequestRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link JoinRequestService} 비즈니스 로직 단위 테스트.
 *
 * <p>참여 요청 생성, 조회, 승인, 거절의 핵심 흐름과 예외 시나리오를 검증한다.
 * Mockito 기반으로 Repository 의존성을 Mock 처리하여 순수 서비스 로직을 테스트한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JoinRequestServiceTest {

    @InjectMocks
    private JoinRequestService joinRequestService;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundParticipantRepository roundParticipantRepository;

    // =========================================================
    // createJoinRequest
    // =========================================================

    @Nested
    @DisplayName("참여 요청 생성 테스트")
    class CreateJoinRequestTest {

        @Test
        @DisplayName("정상 생성 — 활성 세션 + 미가입 + 대기 요청 없으면 PENDING 요청이 반환된다")
        void createJoinRequest_success() {
            // given
            String inviteCode = "ABCD1234";
            Long requesterId = 10L;
            Long sessionId = 1L;

            Session session = createActiveSession(sessionId, 999L);
            JoinRequestCreateRequest request = buildCreateRequest("테스터", "참여 요청합니다");

            JoinRequest saved = buildJoinRequest(100L, sessionId, requesterId, "테스터", "참여 요청합니다");

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(session));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, requesterId))
                    .willReturn(Optional.empty());
            given(joinRequestRepository.findBySessionIdAndRequesterIdAndStatus(
                    sessionId, requesterId, JoinRequestStatus.PENDING))
                    .willReturn(Optional.empty());
            given(joinRequestRepository.save(any(JoinRequest.class))).willReturn(saved);

            // when
            JoinRequestResponse response = joinRequestService.createJoinRequest(inviteCode, requesterId, request);

            // then
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getRequesterId()).isEqualTo(requesterId);
            assertThat(response.getNickname()).isEqualTo("테스터");
            assertThat(response.getMessage()).isEqualTo("참여 요청합니다");
            assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        @DisplayName("message가 null이어도 정상 생성된다")
        void createJoinRequest_nullMessage_success() {
            // given
            String inviteCode = "ABCD1234";
            Long requesterId = 10L;
            Long sessionId = 1L;

            Session session = createActiveSession(sessionId, 999L);
            JoinRequestCreateRequest request = buildCreateRequest("테스터", null);

            JoinRequest saved = buildJoinRequest(100L, sessionId, requesterId, "테스터", null);

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(session));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, requesterId))
                    .willReturn(Optional.empty());
            given(joinRequestRepository.findBySessionIdAndRequesterIdAndStatus(
                    sessionId, requesterId, JoinRequestStatus.PENDING))
                    .willReturn(Optional.empty());
            given(joinRequestRepository.save(any(JoinRequest.class))).willReturn(saved);

            // when
            JoinRequestResponse response = joinRequestService.createJoinRequest(inviteCode, requesterId, request);

            // then
            assertThat(response.getMessage()).isNull();
            assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
        }

        @Test
        @DisplayName("초대 코드에 해당하는 세션이 없으면 EntityNotFoundException이 발생한다")
        void createJoinRequest_sessionNotFound_throwsEntityNotFoundException() {
            // given
            String inviteCode = "NOTFOUND";
            Long requesterId = 10L;
            JoinRequestCreateRequest request = buildCreateRequest("테스터", null);

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.createJoinRequest(inviteCode, requesterId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting(e -> ((EntityNotFoundException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("세션이 ARCHIVED 상태이면 BusinessException(SESSION_ALREADY_ARCHIVED)이 발생한다")
        void createJoinRequest_archivedSession_throwsBusinessException() {
            // given
            String inviteCode = "ARCHIVED";
            Long requesterId = 10L;
            Long sessionId = 1L;

            Session session = createArchivedSession(sessionId, 999L);
            JoinRequestCreateRequest request = buildCreateRequest("테스터", null);

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.createJoinRequest(inviteCode, requesterId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.SESSION_ALREADY_ARCHIVED);
        }

        @Test
        @DisplayName("이미 세션 멤버이면 BusinessException(JOIN_REQUEST_ALREADY_MEMBER)이 발생한다")
        void createJoinRequest_alreadyMember_throwsBusinessException() {
            // given
            String inviteCode = "ABCD1234";
            Long requesterId = 10L;
            Long sessionId = 1L;

            Session session = createActiveSession(sessionId, 999L);
            OrgMember existingMember = buildOrgMember(50L, sessionId, requesterId, "기존멤버");
            JoinRequestCreateRequest request = buildCreateRequest("테스터", null);

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(session));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, requesterId))
                    .willReturn(Optional.of(existingMember));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.createJoinRequest(inviteCode, requesterId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_ALREADY_MEMBER);
        }

        @Test
        @DisplayName("이미 PENDING 요청이 있으면 BusinessException(JOIN_REQUEST_ALREADY_PENDING)이 발생한다")
        void createJoinRequest_alreadyPending_throwsBusinessException() {
            // given
            String inviteCode = "ABCD1234";
            Long requesterId = 10L;
            Long sessionId = 1L;

            Session session = createActiveSession(sessionId, 999L);
            JoinRequest pendingRequest = buildJoinRequest(99L, sessionId, requesterId, "테스터", null);
            JoinRequestCreateRequest request = buildCreateRequest("테스터", null);

            given(sessionRepository.findByInviteCode(inviteCode)).willReturn(Optional.of(session));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, requesterId))
                    .willReturn(Optional.empty());
            given(joinRequestRepository.findBySessionIdAndRequesterIdAndStatus(
                    sessionId, requesterId, JoinRequestStatus.PENDING))
                    .willReturn(Optional.of(pendingRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.createJoinRequest(inviteCode, requesterId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_ALREADY_PENDING);
        }
    }

    // =========================================================
    // getJoinRequests
    // =========================================================

    @Nested
    @DisplayName("참여 요청 조회 테스트")
    class GetJoinRequestsTest {

        @Test
        @DisplayName("status 필터 없이 조회 시 세션의 전체 참여 요청 페이지가 반환된다")
        void getJoinRequests_noStatusFilter_returnsAll() {
            // given
            Long sessionId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            JoinRequest req1 = buildJoinRequest(1L, sessionId, 10L, "유저A", null);
            JoinRequest req2 = buildJoinRequest(2L, sessionId, 20L, "유저B", "안녕");

            given(joinRequestRepository.findBySessionId(sessionId, pageable))
                    .willReturn(new PageImpl<>(List.of(req1, req2), pageable, 2));

            // when
            Page<JoinRequestResponse> responses =
                    joinRequestService.getJoinRequests(sessionId, null, pageable);

            // then
            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getContent().get(0).getRequesterId()).isEqualTo(10L);
            assertThat(responses.getContent().get(1).getRequesterId()).isEqualTo(20L);
            assertThat(responses.getTotalElements()).isEqualTo(2);
            verify(joinRequestRepository).findBySessionId(sessionId, pageable);
            verify(joinRequestRepository, never())
                    .findBySessionIdAndStatus(any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("status=PENDING 필터 조회 시 PENDING 요청만 포함된 페이지가 반환된다")
        void getJoinRequests_withStatusFilter_returnFiltered() {
            // given
            Long sessionId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            JoinRequest pendingReq = buildJoinRequest(1L, sessionId, 10L, "유저A", null);

            given(joinRequestRepository.findBySessionIdAndStatus(sessionId, JoinRequestStatus.PENDING, pageable))
                    .willReturn(new PageImpl<>(List.of(pendingReq), pageable, 1));

            // when
            Page<JoinRequestResponse> responses =
                    joinRequestService.getJoinRequests(sessionId, JoinRequestStatus.PENDING, pageable);

            // then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).getStatus()).isEqualTo(JoinRequestStatus.PENDING);
            verify(joinRequestRepository).findBySessionIdAndStatus(sessionId, JoinRequestStatus.PENDING, pageable);
            verify(joinRequestRepository, never()).findBySessionId(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("참여 요청이 없으면 빈 페이지가 반환된다")
        void getJoinRequests_empty_returnsEmptyPage() {
            // given
            Long sessionId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            given(joinRequestRepository.findBySessionId(sessionId, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            // when
            Page<JoinRequestResponse> responses =
                    joinRequestService.getJoinRequests(sessionId, null, pageable);

            // then
            assertThat(responses.getContent()).isEmpty();
            assertThat(responses.getTotalElements()).isZero();
        }
    }

    // =========================================================
    // approveJoinRequest
    // =========================================================

    @Nested
    @DisplayName("참여 요청 승인 테스트")
    class ApproveJoinRequestTest {

        @Test
        @DisplayName("정상 승인 — PENDING 요청이 APPROVED로 전환되고 OrgMember가 생성된다")
        void approveJoinRequest_success() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;
            Long requesterId = 10L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, requesterId, "테스터", null);
            OrgMember savedMember = buildOrgMember(50L, sessionId, requesterId, "테스터");

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(orgMemberRepository.saveAndFlush(any(OrgMember.class))).willReturn(savedMember);
            given(roundRepository.findBySessionIdAndStatus(sessionId, RoundStatus.OPEN)).willReturn(List.of());

            // when
            JoinRequestResponse response = joinRequestService.approveJoinRequest(sessionId, requestId);

            // then
            assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.APPROVED);
            assertThat(response.getRequesterId()).isEqualTo(requesterId);
            verify(orgMemberRepository).saveAndFlush(any(OrgMember.class));
        }

        @Test
        @DisplayName("승인 시 열린 라운드에 RoundParticipant가 동기화된다")
        void approveJoinRequest_syncsOpenRoundParticipant() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;
            Long requesterId = 10L;
            Long orgMemberId = 50L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, requesterId, "테스터", null);
            OrgMember savedMember = buildOrgMember(orgMemberId, sessionId, requesterId, "테스터");

            Round openRound = Round.builder().sessionId(sessionId).title("라운드1").sortOrder(1).build();
            ReflectionTestUtils.setField(openRound, "id", 10L);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(orgMemberRepository.saveAndFlush(any(OrgMember.class))).willReturn(savedMember);
            given(roundRepository.findBySessionIdAndStatus(sessionId, RoundStatus.OPEN))
                    .willReturn(List.of(openRound));
            given(roundParticipantRepository.findRoundIdsByRoundIdInAndOrgMemberId(
                    List.of(10L), orgMemberId)).willReturn(Set.of());

            // when
            joinRequestService.approveJoinRequest(sessionId, requestId);

            // then
            verify(roundParticipantRepository).saveAll(any(List.class));
        }

        @Test
        @DisplayName("이미 RoundParticipant가 존재하면 중복 저장하지 않는다")
        void approveJoinRequest_skipsDuplicateRoundParticipant() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;
            Long requesterId = 10L;
            Long orgMemberId = 50L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, requesterId, "테스터", null);
            OrgMember savedMember = buildOrgMember(orgMemberId, sessionId, requesterId, "테스터");

            Round openRound = Round.builder().sessionId(sessionId).title("라운드1").sortOrder(1).build();
            ReflectionTestUtils.setField(openRound, "id", 10L);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(orgMemberRepository.saveAndFlush(any(OrgMember.class))).willReturn(savedMember);
            given(roundRepository.findBySessionIdAndStatus(sessionId, RoundStatus.OPEN))
                    .willReturn(List.of(openRound));
            given(roundParticipantRepository.findRoundIdsByRoundIdInAndOrgMemberId(
                    List.of(10L), orgMemberId)).willReturn(Set.of(10L));

            // when
            joinRequestService.approveJoinRequest(sessionId, requestId);

            // then — saveAll은 호출되지만 빈 리스트를 전달한다 (실제 저장 없음)
            verify(roundParticipantRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("닫힌 라운드에는 RoundParticipant를 동기화하지 않는다")
        void approveJoinRequest_skipsClosedRound() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;
            Long requesterId = 10L;
            Long orgMemberId = 50L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, requesterId, "테스터", null);
            OrgMember savedMember = buildOrgMember(orgMemberId, sessionId, requesterId, "테스터");

            // CLOSED 상태 라운드
            Round closedRound = Round.builder()
                    .sessionId(sessionId)
                    .title("닫힌라운드")
                    .sortOrder(1)
                    .build();
            ReflectionTestUtils.setField(closedRound, "id", 20L);
            closedRound.changeStatus(RoundStatus.CLOSED);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(orgMemberRepository.saveAndFlush(any(OrgMember.class))).willReturn(savedMember);
            // DB가 OPEN 라운드만 반환하므로 CLOSED 라운드는 조회 결과에 포함되지 않는다
            given(roundRepository.findBySessionIdAndStatus(sessionId, RoundStatus.OPEN))
                    .willReturn(List.of());

            // when
            joinRequestService.approveJoinRequest(sessionId, requestId);

            // then — OPEN 라운드가 없으므로 RoundParticipant 관련 메서드 호출 없음
            verify(roundParticipantRepository, never())
                    .findRoundIdsByRoundIdInAndOrgMemberId(any(), any());
            verify(roundParticipantRepository, never()).saveAll(any(List.class));
        }

        @Test
        @DisplayName("존재하지 않는 참여 요청 승인 시 EntityNotFoundException이 발생한다")
        void approveJoinRequest_requestNotFound_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long requestId = 999L;

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.approveJoinRequest(sessionId, requestId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting(e -> ((EntityNotFoundException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("요청이 다른 세션에 속하면 EntityNotFoundException이 발생한다")
        void approveJoinRequest_sessionMismatch_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long anotherSessionId = 2L;
            Long requestId = 100L;

            // anotherSessionId 소속 요청
            JoinRequest joinRequest = buildJoinRequest(requestId, anotherSessionId, 10L, "테스터", null);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.approveJoinRequest(sessionId, requestId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting(e -> ((EntityNotFoundException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 요청 승인 시 BusinessException(JOIN_REQUEST_NOT_PENDING)이 발생한다")
        void approveJoinRequest_notPending_throwsBusinessException() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;

            // 이미 APPROVED 상태인 요청
            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, 10L, "테스터", null);
            joinRequest.approve(); // APPROVED 상태로 전환

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.approveJoinRequest(sessionId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("OrgMember 저장 중 DataIntegrityViolationException 발생 시 BusinessException(ALREADY_SESSION_MEMBER)으로 변환된다")
        void approveJoinRequest_dataIntegrityViolation_throwsBusinessException() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, 10L, "테스터", null);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));
            given(orgMemberRepository.saveAndFlush(any(OrgMember.class)))
                    .willThrow(new DataIntegrityViolationException("uk_session_user 위반"));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.approveJoinRequest(sessionId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ALREADY_SESSION_MEMBER);
        }
    }

    // =========================================================
    // rejectJoinRequest
    // =========================================================

    @Nested
    @DisplayName("참여 요청 거절 테스트")
    class RejectJoinRequestTest {

        @Test
        @DisplayName("정상 거절 — PENDING 요청이 REJECTED로 전환된다")
        void rejectJoinRequest_success() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;
            Long requesterId = 10L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, requesterId, "테스터", null);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when
            JoinRequestResponse response = joinRequestService.rejectJoinRequest(sessionId, requestId);

            // then
            assertThat(response.getStatus()).isEqualTo(JoinRequestStatus.REJECTED);
            assertThat(response.getRequesterId()).isEqualTo(requesterId);
            // 거절 시 OrgMember 저장이 발생하지 않아야 한다
            verify(orgMemberRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("존재하지 않는 참여 요청 거절 시 EntityNotFoundException이 발생한다")
        void rejectJoinRequest_requestNotFound_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long requestId = 999L;

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.rejectJoinRequest(sessionId, requestId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting(e -> ((EntityNotFoundException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("요청이 다른 세션에 속하면 EntityNotFoundException이 발생한다")
        void rejectJoinRequest_sessionMismatch_throwsEntityNotFoundException() {
            // given
            Long sessionId = 1L;
            Long anotherSessionId = 2L;
            Long requestId = 100L;

            JoinRequest joinRequest = buildJoinRequest(requestId, anotherSessionId, 10L, "테스터", null);

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.rejectJoinRequest(sessionId, requestId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .extracting(e -> ((EntityNotFoundException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 요청 거절 시 BusinessException(JOIN_REQUEST_NOT_PENDING)이 발생한다")
        void rejectJoinRequest_notPending_throwsBusinessException() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;

            // 이미 REJECTED 상태인 요청
            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, 10L, "테스터", null);
            joinRequest.reject(); // REJECTED 상태로 전환

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.rejectJoinRequest(sessionId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("APPROVED 상태에서 거절 시 BusinessException(JOIN_REQUEST_NOT_PENDING)이 발생한다")
        void rejectJoinRequest_alreadyApproved_throwsBusinessException() {
            // given
            Long sessionId = 1L;
            Long requestId = 100L;

            JoinRequest joinRequest = buildJoinRequest(requestId, sessionId, 10L, "테스터", null);
            joinRequest.approve(); // APPROVED 상태로 전환

            given(joinRequestRepository.findById(requestId)).willReturn(Optional.of(joinRequest));

            // when & then
            assertThatThrownBy(() ->
                    joinRequestService.rejectJoinRequest(sessionId, requestId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    /**
     * 활성(ACTIVE) 상태의 Session 테스트 픽스처를 생성한다.
     *
     * @param sessionId 세션 ID
     * @param ownerId   소유자 ID
     * @return 활성 Session 인스턴스
     */
    private Session createActiveSession(Long sessionId, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .status(SessionStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        return session;
    }

    /**
     * 보관(ARCHIVED) 상태의 Session 테스트 픽스처를 생성한다.
     *
     * @param sessionId 세션 ID
     * @param ownerId   소유자 ID
     * @return 아카이브된 Session 인스턴스
     */
    private Session createArchivedSession(Long sessionId, Long ownerId) {
        Session session = Session.builder()
                .title("완료된 세션")
                .ownerId(ownerId)
                .status(SessionStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.archive(); // ARCHIVED 상태로 전환
        return session;
    }

    /**
     * JoinRequest 테스트 픽스처를 생성한다 (초기 상태: PENDING).
     *
     * @param id          요청 ID
     * @param sessionId   세션 ID
     * @param requesterId 요청자 ID
     * @param nickname    닉네임
     * @param message     메시지
     * @return PENDING 상태의 JoinRequest 인스턴스
     */
    private JoinRequest buildJoinRequest(Long id, Long sessionId, Long requesterId,
                                         String nickname, String message) {
        JoinRequest joinRequest = JoinRequest.builder()
                .sessionId(sessionId)
                .requesterId(requesterId)
                .nickname(nickname)
                .message(message)
                .build();
        ReflectionTestUtils.setField(joinRequest, "id", id);
        return joinRequest;
    }

    /**
     * OrgMember 테스트 픽스처를 생성한다.
     *
     * @param id        멤버 ID
     * @param sessionId 세션 ID
     * @param userId    사용자 ID
     * @param nickname  닉네임
     * @return OrgMember 인스턴스
     */
    private OrgMember buildOrgMember(Long id, Long sessionId, Long userId, String nickname) {
        OrgMember member = OrgMember.builder()
                .sessionId(sessionId)
                .userId(userId)
                .nickname(nickname)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    /**
     * JoinRequestCreateRequest 테스트 픽스처를 생성한다.
     *
     * @param nickname 닉네임
     * @param message  메시지
     * @return JoinRequestCreateRequest 인스턴스
     */
    private JoinRequestCreateRequest buildCreateRequest(String nickname, String message) {
        JoinRequestCreateRequest request = new JoinRequestCreateRequest();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "message", message);
        return request;
    }
}
