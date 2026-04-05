package com.dirtypay.domain.member.service;

import com.dirtypay.domain.member.dto.request.MemberUpdateRequest;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.domain.member.dto.response.MemberSearchResponse;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 조회 성공")
    void getMember_success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "test@test.com", "테스트");

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMember(memberId);

        // then
        assertThat(response.getId()).isEqualTo(memberId);
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getName()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외 발생")
    void getMember_notFound() {
        // given
        Long memberId = 999L;

        given(memberRepository.findById(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMember(memberId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("회원 정보 수정 성공")
    void updateMember_success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "test@test.com", "원래이름");

        MemberUpdateRequest request = new MemberUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "새이름");
        ReflectionTestUtils.setField(request, "profileImage", "new-image.jpg");

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.updateMember(memberId, request);

        // then
        assertThat(response.getName()).isEqualTo("새이름");
        assertThat(response.getProfileImage()).isEqualTo("new-image.jpg");
    }

    @Test
    @DisplayName("회원 삭제 성공")
    void deleteMember_success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, "test@test.com", "테스트");

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        // when
        memberService.deleteMember(memberId);

        // then
        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 회원 수정 시 예외 발생")
    void updateMember_notFound_throwsException() {
        // given
        Long memberId = 999L;
        MemberUpdateRequest request = new MemberUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "새이름");

        given(memberRepository.findById(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(memberId, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원 삭제 시 예외 발생")
    void deleteMember_notFound_throwsException() {
        // given
        Long memberId = 999L;

        given(memberRepository.findById(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.deleteMember(memberId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("회원 검색 성공")
    void searchMembers_success() {
        // given
        String query = "test";
        Pageable pageable = PageRequest.of(0, 20);
        Member member = createMember(1L, "test@test.com", "테스트");
        Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

        given(memberRepository.searchByKeyword(query, pageable))
                .willReturn(memberPage);

        // when
        Page<MemberSearchResponse> result = memberService.searchMembers(query, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@test.com");
        assertThat(result.getContent().get(0).getName()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("빈 검색어로 검색 시 예외 발생")
    void searchMembers_emptyQuery_throwsException() {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        // when & then
        assertThatThrownBy(() -> memberService.searchMembers("", pageable))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> memberService.searchMembers("   ", pageable))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> memberService.searchMembers(null, pageable))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("101자 이상의 검색어로 검색 시 예외 발생")
    void searchMembers_queryTooLong_throwsException() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        // 101자 문자열 생성
        String tooLongQuery = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> memberService.searchMembers(tooLongQuery, pageable))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("정확히 100자 검색어는 정상 처리됨")
    void searchMembers_queryExactly100Chars_success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        String maxLengthQuery = "a".repeat(100);
        Page<Member> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(memberRepository.searchByKeyword(maxLengthQuery, pageable))
                .willReturn(emptyPage);

        // when
        Page<MemberSearchResponse> result = memberService.searchMembers(maxLengthQuery, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("검색 결과 없음 - 빈 Page 반환")
    void searchMembers_noResult_returnsEmptyPage() {
        // given
        String query = "nonexistent";
        Pageable pageable = PageRequest.of(0, 20);
        Page<Member> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(memberRepository.searchByKeyword(query, pageable))
                .willReturn(emptyPage);

        // when
        Page<MemberSearchResponse> result = memberService.searchMembers(query, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private Member createMember(Long id, String email, String name) {
        Member member = Member.builder()
                .email(email)
                .password("password123")
                .name(name)
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
