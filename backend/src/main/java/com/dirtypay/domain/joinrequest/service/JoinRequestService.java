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
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 참여 요청 서비스.
 *
 * <p>세션 참여 요청의 생성, 조회, 승인, 거절 비즈니스 로직을 처리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JoinRequestService {

    private final JoinRequestRepository joinRequestRepository;
    private final SessionRepository sessionRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final RoundRepository roundRepository;
    private final RoundParticipantRepository roundParticipantRepository;

    /**
     * 참여 요청을 생성한다.
     *
     * <p>초대 코드로 세션을 조회하고, 검증 후 PENDING 상태의 참여 요청을 생성한다.</p>
     *
     * @param inviteCode  초대 코드
     * @param requesterId 요청자 ID (Member.id)
     * @param request     참여 요청 생성 요청
     * @return 생성된 참여 요청 응답
     * @throws EntityNotFoundException 세션을 찾을 수 없는 경우
     * @throws BusinessException       세션이 비활성인 경우, 이미 참여 중인 경우, 이미 대기 중인 요청이 있는 경우
     */
    @Transactional
    public JoinRequestResponse createJoinRequest(String inviteCode, Long requesterId,
                                                  JoinRequestCreateRequest request) {
        Session session = this.sessionRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));

        this.verifySessionActive(session);
        this.verifyNotAlreadyMember(session.getId(), requesterId);
        this.verifyNoPendingRequest(session.getId(), requesterId);

        JoinRequest joinRequest = JoinRequest.builder()
                .sessionId(session.getId())
                .requesterId(requesterId)
                .nickname(request.getNickname())
                .message(request.getMessage())
                .build();

        JoinRequest saved = this.joinRequestRepository.save(joinRequest);
        return JoinRequestResponse.from(saved);
    }

    /**
     * 세션의 참여 요청 목록을 페이지 단위로 조회한다.
     *
     * @param sessionId 세션 ID
     * @param status    요청 상태 필터 (null이면 전체 조회)
     * @param pageable  페이지 요청 정보
     * @return 참여 요청 페이지
     */
    public Page<JoinRequestResponse> getJoinRequests(Long sessionId, JoinRequestStatus status, Pageable pageable) {
        if (status != null) {
            return this.joinRequestRepository.findBySessionIdAndStatus(sessionId, status, pageable)
                    .map(JoinRequestResponse::from);
        }
        return this.joinRequestRepository.findBySessionId(sessionId, pageable)
                .map(JoinRequestResponse::from);
    }

    /**
     * 참여 요청을 승인한다.
     *
     * <p>상태를 APPROVED로 변경하고, 세션에 OrgMember를 자동 생성한다.</p>
     *
     * <p>두 관리자가 동시에 같은 요청을 승인하더라도, DB의 {@code uk_session_user}
     * UNIQUE 인덱스가 OrgMember 중복 생성을 방지한다.
     * {@link DataIntegrityViolationException} 발생 시 {@link ErrorCode#ALREADY_SESSION_MEMBER}로 변환한다.</p>
     *
     * @param sessionId 세션 ID
     * @param requestId 참여 요청 ID
     * @return 승인된 참여 요청 응답
     * @throws EntityNotFoundException        참여 요청을 찾을 수 없는 경우
     * @throws BusinessException              PENDING 상태가 아닌 경우, 또는 동시 승인으로 이미 멤버가 된 경우
     */
    @Transactional
    public JoinRequestResponse approveJoinRequest(Long sessionId, Long requestId) {
        JoinRequest joinRequest = this.findJoinRequestById(requestId);
        this.verifyBelongsToSession(joinRequest, sessionId);

        joinRequest.approve();

        OrgMember orgMember = OrgMember.builder()
                .sessionId(sessionId)
                .userId(joinRequest.getRequesterId())
                .nickname(joinRequest.getNickname())
                .isActive(true)
                .build();

        OrgMember saved;
        try {
            saved = this.orgMemberRepository.saveAndFlush(orgMember);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_SESSION_MEMBER, e);
        }

        // 열려 있는 라운드에 RoundParticipant 동기화 (중복 방어)
        // - IN 쿼리 2회로 N+1 제거: (1) OPEN 라운드 일괄 조회, (2) 기존 참여 라운드 일괄 조회
        List<Round> openRounds = this.roundRepository.findBySessionIdAndStatus(sessionId, RoundStatus.OPEN);
        if (!openRounds.isEmpty()) {
            List<Long> openRoundIds = openRounds.stream().map(Round::getId).toList();
            Set<Long> alreadyJoinedRoundIds = this.roundParticipantRepository
                    .findRoundIdsByRoundIdInAndOrgMemberId(openRoundIds, saved.getId());
            List<RoundParticipant> newParticipants = openRoundIds.stream()
                    .filter(roundId -> !alreadyJoinedRoundIds.contains(roundId))
                    .map(roundId -> RoundParticipant.builder().roundId(roundId).orgMemberId(saved.getId()).build())
                    .toList();
            this.roundParticipantRepository.saveAll(newParticipants);
        }

        return JoinRequestResponse.from(joinRequest);
    }

    /**
     * 참여 요청을 거절한다.
     *
     * @param sessionId 세션 ID
     * @param requestId 참여 요청 ID
     * @return 거절된 참여 요청 응답
     * @throws EntityNotFoundException 참여 요청을 찾을 수 없는 경우
     * @throws BusinessException       PENDING 상태가 아닌 경우
     */
    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long sessionId, Long requestId) {
        JoinRequest joinRequest = this.findJoinRequestById(requestId);
        this.verifyBelongsToSession(joinRequest, sessionId);

        joinRequest.reject();

        return JoinRequestResponse.from(joinRequest);
    }

    private JoinRequest findJoinRequestById(Long requestId) {
        return this.joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND));
    }

    private void verifySessionActive(Session session) {
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ARCHIVED);
        }
    }

    private void verifyNotAlreadyMember(Long sessionId, Long userId) {
        this.orgMemberRepository.findBySessionIdAndUserId(sessionId, userId)
                .ifPresent(member -> {
                    throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_MEMBER);
                });
    }

    private void verifyNoPendingRequest(Long sessionId, Long requesterId) {
        this.joinRequestRepository.findBySessionIdAndRequesterIdAndStatus(
                sessionId, requesterId, JoinRequestStatus.PENDING)
                .ifPresent(request -> {
                    throw new BusinessException(ErrorCode.JOIN_REQUEST_ALREADY_PENDING);
                });
    }

    private void verifyBelongsToSession(JoinRequest joinRequest, Long sessionId) {
        if (!joinRequest.getSessionId().equals(sessionId)) {
            throw new EntityNotFoundException(ErrorCode.JOIN_REQUEST_NOT_FOUND);
        }
    }

}
