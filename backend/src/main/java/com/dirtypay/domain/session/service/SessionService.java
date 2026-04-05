package com.dirtypay.domain.session.service;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.dto.request.SessionCreateRequest;
import com.dirtypay.domain.session.dto.request.SessionUpdateRequest;
import com.dirtypay.domain.session.dto.response.SessionResponse;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 세션 서비스.
 *
 * <p>세션의 생성, 조회, 수정, 삭제(Soft Delete) 비즈니스 로직을 처리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MemberRepository memberRepository;
    private final RoundRepository roundRepository;
    private final OrderRepository orderRepository;

    /**
     * 새로운 세션을 생성한다.
     *
     * <p>세션 생성 후 루트 노드와 소유자 OrgMember를 자동으로 생성하여
     * 조직 관리에서 바로 관리할 수 있도록 한다.</p>
     *
     * @param request 세션 생성 요청
     * @param ownerId 세션 소유자 ID
     * @return 생성된 세션 응답
     */
    @Transactional
    public SessionResponse createSession(SessionCreateRequest request, Long ownerId) {
        Session session = Session.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .ownerId(ownerId)
                .build();

        Session saved = this.sessionRepository.save(session);

        this.initializeOrganization(saved.getId(), ownerId);

        return SessionResponse.from(saved);
    }

    /**
     * 현재 사용자가 참여 중인 활성 세션 목록을 조회한다.
     *
     * <p>소유한 세션과 참여자로 등록된 세션을 모두 포함하며,
     * 각 세션에 대해 참여 인원, 라운드 수, 총 금액 집계를 포함한다.</p>
     *
     * @param userId 사용자 ID
     * @return 활성 세션 목록
     */
    public List<SessionResponse> getSessions(Long userId) {
        return this.sessionRepository.findByMemberUserIdAndStatus(userId, SessionStatus.ACTIVE).stream()
                .map(this::toSessionResponseWithAggregation)
                .toList();
    }

    /**
     * 현재 사용자가 참여 중인 만료(ARCHIVED) 세션 목록을 조회한다.
     *
     * <p>소유한 세션과 참여자로 등록된 세션을 모두 포함하며,
     * 각 세션에 대해 참여 인원, 라운드 수, 총 금액 집계를 포함한다.</p>
     *
     * @param userId 사용자 ID
     * @return 만료 세션 목록
     */
    public List<SessionResponse> getArchivedSessions(Long userId) {
        return this.sessionRepository.findByMemberUserIdAndStatus(userId, SessionStatus.ARCHIVED).stream()
                .map(this::toSessionResponseWithAggregation)
                .toList();
    }

    /**
     * 세션 상세 정보를 조회한다.
     *
     * <p>참여 인원, 라운드 수, 총 금액 집계를 포함한다.</p>
     *
     * @param sessionId 세션 ID
     * @return 세션 응답
     */
    public SessionResponse getSession(Long sessionId) {
        Session session = this.findSessionById(sessionId);
        return this.toSessionResponseWithAggregation(session);
    }

    /**
     * 세션 정보를 수정한다.
     *
     * @param sessionId 세션 ID
     * @param request   수정 요청
     * @return 수정된 세션 응답
     * @throws BusinessException 이미 완료된 세션인 경우
     */
    @Transactional
    public SessionResponse updateSession(Long sessionId, SessionUpdateRequest request) {
        Session session = this.findSessionById(sessionId);
        this.verifySessionActive(session);

        session.update(request.getTitle(), request.getDescription(),
                request.getStartDate(), request.getEndDate());

        return SessionResponse.from(session);
    }

    /**
     * 초대 코드로 세션을 조회한다.
     *
     * <p>참여 인원, 라운드 수, 총 금액 집계를 포함한다.</p>
     *
     * @param inviteCode 초대 코드
     * @return 세션 응답
     */
    public SessionResponse getSessionByInviteCode(String inviteCode) {
        Session session = this.sessionRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
        return this.toSessionResponseWithAggregation(session);
    }

    /**
     * 세션을 완료(ARCHIVED) 상태로 변경한다.
     *
     * <p>하위 OPEN 상태의 라운드를 모두 CLOSED로 자동 변경한다.</p>
     *
     * @param sessionId 세션 ID
     * @return 변경된 세션 응답
     * @throws BusinessException 이미 완료된 세션인 경우
     */
    @Transactional
    public SessionResponse archiveSession(Long sessionId) {
        Session session = this.findSessionById(sessionId);
        this.verifySessionActive(session);

        session.archive();

        this.roundRepository.findBySessionId(sessionId).stream()
                .filter(Round::isOpen)
                .forEach(round -> round.changeStatus(RoundStatus.CLOSED));

        return this.toSessionResponseWithAggregation(session);
    }

    /**
     * 세션을 삭제한다. (Soft Delete)
     *
     * <p>삭제는 세션 상태와 무관하게 항상 허용된다.</p>
     *
     * @param sessionId 세션 ID
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        Session session = this.findSessionById(sessionId);
        session.delete();
    }

    /**
     * 세션의 조직 구조를 초기화한다.
     *
     * <p>소유자를 OrgMember로 등록한다.</p>
     *
     * @param sessionId 세션 ID
     * @param ownerId   소유자 ID (Member.id)
     */
    private void initializeOrganization(Long sessionId, Long ownerId) {
        Member owner = this.memberRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        this.orgMemberRepository.save(
                OrgMember.builder()
                        .sessionId(sessionId)
                        .userId(ownerId)
                        .nickname(owner.getName())
                        .isActive(true)
                        .build());
    }

    private void verifySessionActive(Session session) {
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ARCHIVED);
        }
    }

    private Session findSessionById(Long sessionId) {
        return this.sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
    }

    private SessionResponse toSessionResponseWithAggregation(Session session) {
        long memberCount = this.orgMemberRepository.countBySessionId(session.getId());
        long roundCount = this.roundRepository.countBySessionId(session.getId());
        BigDecimal totalAmount = this.orderRepository.sumTotalPriceBySessionId(session.getId());

        return SessionResponse.from(session, memberCount, roundCount, totalAmount);
    }
}
