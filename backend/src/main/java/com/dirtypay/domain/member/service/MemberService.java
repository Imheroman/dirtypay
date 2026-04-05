package com.dirtypay.domain.member.service;

import com.dirtypay.domain.member.dto.request.MemberUpdateRequest;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.domain.member.dto.response.MemberSearchResponse;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관리 서비스.
 *
 * <p>회원 조회, 수정, 삭제 기능을 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 회원 정보를 조회한다.
     *
     * @param id 조회할 회원의 ID
     * @return 회원 정보 응답 DTO
     * @throws EntityNotFoundException 회원이 존재하지 않는 경우
     */
    public MemberResponse getMember(Long id) {
        Member member = this.findMemberById(id);
        return MemberResponse.from(member);
    }

    /**
     * 회원 정보를 수정한다.
     *
     * @param id      수정할 회원의 ID
     * @param request 회원 정보 수정 요청 DTO
     * @return 수정된 회원 정보 응답 DTO
     * @throws EntityNotFoundException 회원이 존재하지 않는 경우
     */
    @Transactional
    public MemberResponse updateMember(Long id, MemberUpdateRequest request) {
        Member member = this.findMemberById(id);
        member.updateProfile(request.getName(), request.getProfileImage());
        return MemberResponse.from(member);
    }

    /**
     * 회원을 삭제한다. (Soft Delete)
     *
     * @param id 삭제할 회원의 ID
     * @throws EntityNotFoundException 회원이 존재하지 않는 경우
     */
    @Transactional
    public void deleteMember(Long id) {
        Member member = this.findMemberById(id);
        member.delete();
    }

    /**
     * 이메일 또는 이름으로 회원을 검색한다.
     *
     * <p>검색어는 비어있을 수 없으며, 최대 100자까지 허용된다.</p>
     *
     * @param query    검색 키워드 (1자 이상, 최대 100자)
     * @param pageable 페이징 정보
     * @return 검색된 회원 Page
     * @throws BusinessException 검색어가 비어있거나 100자를 초과하는 경우
     */
    public Page<MemberSearchResponse> searchMembers(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.MEMBER_SEARCH_QUERY_EMPTY);
        }
        String trimmed = query.trim();
        if (trimmed.length() > 100) {
            throw new BusinessException(ErrorCode.MEMBER_SEARCH_QUERY_TOO_LONG);
        }
        return this.memberRepository.searchByKeyword(trimmed, pageable)
                .map(MemberSearchResponse::from);
    }

    /**
     * ID로 회원을 조회한다.
     *
     * @param id 조회할 회원의 ID
     * @return 조회된 회원 엔티티
     * @throws EntityNotFoundException 회원이 존재하지 않는 경우
     */
    private Member findMemberById(Long id) {
        return this.memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
