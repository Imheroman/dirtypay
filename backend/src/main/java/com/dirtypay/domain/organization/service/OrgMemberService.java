package com.dirtypay.domain.organization.service;

import com.dirtypay.domain.organization.dto.request.OrgMemberCreateRequest;
import com.dirtypay.domain.organization.dto.request.OrgMemberUpdateRequest;
import com.dirtypay.domain.organization.dto.response.OrgMemberResponse;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 조직도 멤버 서비스.
 *
 * <p>조직도 멤버의 생성, 조회, 수정, 삭제(Soft Delete) 비즈니스 로직을 처리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgMemberService {

    private final OrgMemberRepository orgMemberRepository;
    private final RoundRepository roundRepository;
    private final RoundParticipantRepository roundParticipantRepository;

    /**
     * 새로운 멤버를 생성한다.
     *
     * @param sessionId 세션 ID
     * @param request   멤버 생성 요청
     * @return 생성된 멤버 응답
     */
    @Transactional
    public OrgMemberResponse createMember(Long sessionId, OrgMemberCreateRequest request) {
        OrgMember member = OrgMember.builder()
                .sessionId(sessionId)
                .userId(request.getUserId())
                .nickname(request.getNickname())
                .build();

        OrgMember saved = this.orgMemberRepository.save(member);

        // 열려 있는 라운드에 RoundParticipant 동기화 (중복 방어)
        this.roundRepository.findBySessionId(sessionId).stream()
                .filter(Round::isOpen)
                .forEach(round -> {
                    if (!this.roundParticipantRepository.existsByRoundIdAndOrgMemberId(
                            round.getId(), saved.getId())) {
                        this.roundParticipantRepository.save(
                                RoundParticipant.builder().roundId(round.getId()).orgMemberId(saved.getId()).build());
                    }
                });

        return OrgMemberResponse.from(saved);
    }

    /**
     * 세션의 전체 멤버를 조회한다.
     *
     * <p>인증된 사용자라면 소유자 여부와 무관하게 조회할 수 있다.</p>
     *
     * @param sessionId 세션 ID
     * @return 멤버 응답 목록
     */
    public List<OrgMemberResponse> getMembersBySessionId(Long sessionId) {
        List<OrgMember> members = this.orgMemberRepository.findBySessionId(sessionId);
        return members.stream()
                .map(OrgMemberResponse::from)
                .toList();
    }

    /**
     * 멤버 정보를 수정한다.
     *
     * @param memberId 멤버 ID
     * @param request  멤버 수정 요청
     * @return 수정된 멤버 응답
     */
    @Transactional
    public OrgMemberResponse updateMember(Long memberId, OrgMemberUpdateRequest request) {
        OrgMember member = this.findMemberById(memberId);
        member.update(request.getNickname(), request.getIsActive());

        return OrgMemberResponse.from(member);
    }

    /**
     * OrgMember에 시스템 회원을 연결한다.
     *
     * <p>userId가 null인 OrgMember에 인증된 사용자의 회원 ID를 사후 연결한다.</p>
     *
     * @param memberId 멤버 ID
     * @param userId   연결할 회원 ID
     * @return 연결된 멤버 응답
     */
    @Transactional
    public OrgMemberResponse linkMemberToUser(Long memberId, Long userId) {
        OrgMember member = this.findMemberById(memberId);
        member.linkUser(userId);
        return OrgMemberResponse.from(member);
    }

    /**
     * 멤버를 삭제한다. (Soft Delete)
     *
     * @param memberId 멤버 ID
     */
    @Transactional
    public void deleteMember(Long memberId) {
        OrgMember member = this.findMemberById(memberId);
        member.delete();
    }

    /**
     * 멤버 ID 목록에 대한 닉네임 맵을 조회한다.
     *
     * @param memberIds 멤버 ID 목록
     * @return 멤버 ID → 닉네임 맵
     */
    public Map<Long, String> getNicknameMap(List<Long> memberIds) {
        return this.orgMemberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(OrgMember::getId, OrgMember::getNickname));
    }

    private OrgMember findMemberById(Long memberId) {
        return this.orgMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
