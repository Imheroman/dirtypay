package com.dirtypay.domain.round.service;

import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.dto.request.RoundCreateRequest;
import com.dirtypay.domain.round.dto.request.RoundStatusChangeRequest;
import com.dirtypay.domain.round.dto.request.RoundUpdateRequest;
import com.dirtypay.domain.round.dto.response.RoundParticipantResponse;
import com.dirtypay.domain.round.dto.response.RoundResponse;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 라운드 서비스.
 *
 * <p>라운드의 CRUD, 참여자 초기화, 상태 변경, 참여자 제외/포함 비즈니스 로직을 처리한다.
 * 가게(storeId)가 변경되면 주문 존재 여부를 검증하여 정합성을 보장한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoundService {

    private final RoundRepository roundRepository;
    private final RoundParticipantRepository roundParticipantRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgMemberService orgMemberService;
    private final OrderRepository orderRepository;
    private final SessionRepository sessionRepository;

    /**
     * 새로운 라운드를 생성하고 참여자를 초기화한다.
     *
     * <p>라운드를 저장하고, 해당 세션의 OrgMember 스냅샷으로
     * RoundParticipant를 일괄 생성한다.</p>
     *
     * @param sessionId 세션 ID
     * @param request   라운드 생성 요청
     * @return 생성된 라운드 응답
     */
    @Transactional
    public RoundResponse createRound(Long sessionId, RoundCreateRequest request) {
        this.verifySessionActive(sessionId);

        long count = this.roundRepository.countBySessionId(sessionId);
        if (count >= 10) {
            throw new BusinessException(ErrorCode.ROUND_LIMIT_EXCEEDED);
        }

        Round round = Round.builder()
                .sessionId(sessionId)
                .title(request.getTitle())
                .place(request.getPlace())
                .roundDate(request.getRoundDate())
                .sortOrder(request.getSortOrder())
                .storeId(request.getStoreId())
                .build();

        Round saved = this.roundRepository.save(round);

        this.initializeParticipants(saved.getId(), sessionId);

        return RoundResponse.from(saved);
    }

    /**
     * 세션의 라운드 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 라운드 목록
     */
    public List<RoundResponse> getRounds(Long sessionId) {
        return this.roundRepository.findBySessionIdOrderBySortOrderAsc(sessionId).stream()
                .map(this::toRoundResponseWithAggregation)
                .toList();
    }

    /**
     * 라운드 상세 정보를 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 라운드 응답
     */
    public RoundResponse getRound(Long roundId) {
        Round round = this.findRoundById(roundId);
        return this.toRoundResponseWithAggregation(round);
    }

    /**
     * 라운드 정보를 수정한다.
     *
     * <p>CLOSED 라운드는 sortOrder만 변경 가능하다.
     * 주문이 존재하는 라운드의 가게(storeId)는 변경할 수 없다.</p>
     *
     * @param roundId 라운드 ID
     * @param request 수정 요청
     * @return 수정된 라운드 응답
     * @throws BusinessException CLOSED 라운드에서 sortOrder 외 필드를 변경하려는 경우
     * @throws BusinessException 주문이 존재하는 라운드의 가게를 변경하려는 경우
     */
    @Transactional
    public RoundResponse updateRound(Long roundId, RoundUpdateRequest request) {
        Round round = this.findRoundById(roundId);

        if (!round.isOpen()) {
            boolean onlySortOrderChanged = round.getTitle().equals(request.getTitle())
                    && Objects.equals(round.getPlace(), request.getPlace())
                    && Objects.equals(round.getRoundDate(), request.getRoundDate())
                    && Objects.equals(round.getStoreId(), request.getStoreId());

            if (!onlySortOrderChanged) {
                throw new BusinessException(ErrorCode.ROUND_ALREADY_CLOSED);
            }
        }

        boolean storeChanged = !Objects.equals(round.getStoreId(), request.getStoreId());
        if (storeChanged) {
            if (this.orderRepository.existsByRoundId(roundId)) {
                throw new BusinessException(ErrorCode.ROUND_HAS_ORDERS);
            }
        }

        round.update(request.getTitle(), request.getPlace(),
                request.getRoundDate(), request.getSortOrder(), request.getStoreId());

        return RoundResponse.from(round);
    }

    /**
     * 라운드를 삭제한다. (Soft Delete)
     *
     * <p>삭제는 라운드 상태와 무관하게 항상 허용된다.</p>
     *
     * @param roundId 라운드 ID
     */
    @Transactional
    public void deleteRound(Long roundId) {
        Round round = this.findRoundById(roundId);
        round.delete();
    }

    /**
     * 라운드 상태를 변경한다.
     *
     * @param roundId 라운드 ID
     * @param request 상태 변경 요청
     * @return 변경된 라운드 응답
     * @throws BusinessException 부모 세션이 ARCHIVED 상태인 경우
     */
    @Transactional
    public RoundResponse changeStatus(Long roundId, RoundStatusChangeRequest request) {
        Round round = this.findRoundById(roundId);
        this.verifySessionActive(round.getSessionId());
        round.changeStatus(request.getStatus());

        return RoundResponse.from(round);
    }

    /**
     * 라운드의 참여자 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 참여자 목록
     */
    public List<RoundParticipantResponse> getParticipants(Long roundId) {
        List<RoundParticipant> participants = this.roundParticipantRepository
                .findByRoundId(roundId);

        List<Long> orgMemberIds = participants.stream()
                .map(RoundParticipant::getOrgMemberId)
                .toList();

        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(orgMemberIds);

        return participants.stream()
                .map(p -> RoundParticipantResponse.from(p, nicknameMap.getOrDefault(p.getOrgMemberId(), "")))
                .toList();
    }

    /**
     * 참여자를 정산에서 제외한다.
     *
     * @param roundId       라운드 ID
     * @param participantId 참여자 ID
     * @return 변경된 참여자 응답
     */
    @Transactional
    public RoundParticipantResponse excludeParticipant(Long roundId, Long participantId) {
        Round round = this.findRoundById(roundId);
        round.verifyOpen();

        RoundParticipant participant = this.findParticipantById(participantId);
        participant.exclude();

        String nickname = this.orgMemberRepository.findById(participant.getOrgMemberId())
                .map(OrgMember::getNickname)
                .orElse("");

        return RoundParticipantResponse.from(participant, nickname);
    }

    /**
     * 참여자를 정산에 포함한다.
     *
     * @param roundId       라운드 ID
     * @param participantId 참여자 ID
     * @return 변경된 참여자 응답
     */
    @Transactional
    public RoundParticipantResponse includeParticipant(Long roundId, Long participantId) {
        Round round = this.findRoundById(roundId);
        round.verifyOpen();

        RoundParticipant participant = this.findParticipantById(participantId);
        participant.include();

        String nickname = this.orgMemberRepository.findById(participant.getOrgMemberId())
                .map(OrgMember::getNickname)
                .orElse("");

        return RoundParticipantResponse.from(participant, nickname);
    }

    private void initializeParticipants(Long roundId, Long sessionId) {
        List<OrgMember> orgMembers = this.orgMemberRepository.findBySessionId(sessionId);

        List<RoundParticipant> participants = orgMembers.stream()
                .map(member -> RoundParticipant.builder()
                        .roundId(roundId)
                        .orgMemberId(member.getId())
                        .build())
                .toList();

        this.roundParticipantRepository.saveAll(participants);
    }

    private Round findRoundById(Long roundId) {
        return this.roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_NOT_FOUND));
    }

    private RoundParticipant findParticipantById(Long participantId) {
        return this.roundParticipantRepository.findById(participantId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_PARTICIPANT_NOT_FOUND));
    }

    private void verifySessionActive(Long sessionId) {
        Session session = this.sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ARCHIVED);
        }
    }

    private RoundResponse toRoundResponseWithAggregation(Round round) {
        BigDecimal totalAmount = this.orderRepository.sumTotalPriceByRoundId(round.getId());
        long participantCount = this.roundParticipantRepository.countByRoundId(round.getId());

        return RoundResponse.from(round, totalAmount, participantCount);
    }
}
