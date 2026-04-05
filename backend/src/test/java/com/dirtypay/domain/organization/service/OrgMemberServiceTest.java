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
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrgMemberServiceTest {

    @InjectMocks
    private OrgMemberService orgMemberService;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundParticipantRepository roundParticipantRepository;

    @Nested
    @DisplayName("멤버 생성 테스트")
    class CreateMemberTest {

        @Test
        @DisplayName("멤버 생성 성공 (userId 포함)")
        void createMember_withUserId_success() {
            // given
            Long sessionId = 1L;
            Long userId = 10L;

            OrgMemberCreateRequest request = new OrgMemberCreateRequest();
            ReflectionTestUtils.setField(request, "nickname", "홍길동");
            ReflectionTestUtils.setField(request, "userId", userId);

            OrgMember savedMember = createOrgMember(1L, sessionId, userId, "홍길동");

            given(orgMemberRepository.save(any(OrgMember.class)))
                    .willReturn(savedMember);

            // when
            OrgMemberResponse response = orgMemberService.createMember(sessionId, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getSessionId()).isEqualTo(sessionId);
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getNickname()).isEqualTo("홍길동");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("비회원 멤버 생성 성공 (userId=null)")
        void createMember_withoutUserId_success() {
            // given
            Long sessionId = 1L;

            OrgMemberCreateRequest request = new OrgMemberCreateRequest();
            ReflectionTestUtils.setField(request, "nickname", "비회원참여자");

            OrgMember savedMember = createOrgMember(1L, sessionId, null, "비회원참여자");

            given(orgMemberRepository.save(any(OrgMember.class)))
                    .willReturn(savedMember);

            // when
            OrgMemberResponse response = orgMemberService.createMember(sessionId, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUserId()).isNull();
            assertThat(response.getNickname()).isEqualTo("비회원참여자");
        }

        @Test
        @DisplayName("열린 라운드 존재 시 RoundParticipant가 저장된다")
        void createMember_syncsRoundParticipant() {
            // given
            Long sessionId = 1L;

            OrgMemberCreateRequest request = new OrgMemberCreateRequest();
            ReflectionTestUtils.setField(request, "nickname", "참여자");

            OrgMember savedMember = createOrgMember(1L, sessionId, null, "참여자");

            Round openRound = Round.builder().sessionId(sessionId).title("라운드").sortOrder(1).build();
            ReflectionTestUtils.setField(openRound, "id", 10L);

            given(orgMemberRepository.save(any(OrgMember.class))).willReturn(savedMember);
            given(roundRepository.findBySessionId(sessionId)).willReturn(List.of(openRound));
            given(roundParticipantRepository.existsByRoundIdAndOrgMemberId(10L, 1L))
                    .willReturn(false);

            // when
            orgMemberService.createMember(sessionId, request);

            // then
            verify(roundParticipantRepository).save(any(RoundParticipant.class));
        }

        @Test
        @DisplayName("이미 RoundParticipant가 존재하면 저장하지 않는다")
        void createMember_skipsDuplicateRoundParticipant() {
            // given
            Long sessionId = 1L;

            OrgMemberCreateRequest request = new OrgMemberCreateRequest();
            ReflectionTestUtils.setField(request, "nickname", "참여자");

            OrgMember savedMember = createOrgMember(1L, sessionId, null, "참여자");

            Round openRound = Round.builder().sessionId(sessionId).title("라운드").sortOrder(1).build();
            ReflectionTestUtils.setField(openRound, "id", 10L);

            given(orgMemberRepository.save(any(OrgMember.class))).willReturn(savedMember);
            given(roundRepository.findBySessionId(sessionId)).willReturn(List.of(openRound));
            given(roundParticipantRepository.existsByRoundIdAndOrgMemberId(10L, 1L))
                    .willReturn(true);

            // when
            orgMemberService.createMember(sessionId, request);

            // then
            verify(roundParticipantRepository, never()).save(any(RoundParticipant.class));
        }
    }

    @Nested
    @DisplayName("세션 전체 멤버 조회 테스트")
    class GetMembersBySessionIdTest {

        @Test
        @DisplayName("세션 전체 멤버 조회 성공")
        void getMembersBySessionId_success() {
            // given
            Long sessionId = 1L;
            OrgMember member1 = createOrgMember(1L, sessionId, null, "홍길동");
            OrgMember member2 = createOrgMember(2L, sessionId, null, "김철수");

            given(orgMemberRepository.findBySessionId(sessionId))
                    .willReturn(List.of(member1, member2));

            // when
            List<OrgMemberResponse> responses = orgMemberService.getMembersBySessionId(sessionId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getNickname()).isEqualTo("홍길동");
            assertThat(responses.get(1).getNickname()).isEqualTo("김철수");
        }

        @Test
        @DisplayName("멤버 없으면 빈 목록 반환")
        void getMembersBySessionId_empty_success() {
            // given
            Long sessionId = 1L;

            given(orgMemberRepository.findBySessionId(sessionId))
                    .willReturn(List.of());

            // when
            List<OrgMemberResponse> responses = orgMemberService.getMembersBySessionId(sessionId);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("멤버 수정 테스트")
    class UpdateMemberTest {

        @Test
        @DisplayName("수정 성공 (nickname + isActive)")
        void updateMember_success() {
            // given
            Long memberId = 1L;
            OrgMember member = createOrgMember(memberId, 1L, null, "원래이름");

            OrgMemberUpdateRequest request = new OrgMemberUpdateRequest();
            ReflectionTestUtils.setField(request, "nickname", "새이름");
            ReflectionTestUtils.setField(request, "isActive", false);

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.of(member));

            // when
            OrgMemberResponse response = orgMemberService.updateMember(memberId, request);

            // then
            assertThat(response.getNickname()).isEqualTo("새이름");
            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 멤버 수정 시 실패")
        void updateMember_notFound_failure() {
            // given
            Long memberId = 999L;

            OrgMemberUpdateRequest request = new OrgMemberUpdateRequest();
            ReflectionTestUtils.setField(request, "nickname", "새이름");

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orgMemberService.updateMember(memberId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("멤버-회원 연결 테스트")
    class LinkMemberToUserTest {

        @Test
        @DisplayName("연결 성공 (userId null → non-null)")
        void linkMemberToUser_success() {
            // given
            Long memberId = 1L;
            Long userId = 10L;
            OrgMember member = createOrgMember(memberId, 1L, null, "비회원참여자");

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.of(member));

            // when
            OrgMemberResponse response = orgMemberService.linkMemberToUser(memberId, userId);

            // then
            assertThat(response.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 멤버 연결 시 EntityNotFoundException 발생")
        void linkMemberToUser_memberNotFound_failure() {
            // given
            Long memberId = 999L;

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orgMemberService.linkMemberToUser(memberId, 10L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("이미 회원이 연결된 멤버에 재연결 시 BusinessException 발생")
        void linkMemberToUser_alreadyLinked_failure() {
            // given
            Long memberId = 1L;
            OrgMember member = createOrgMember(memberId, 1L, 10L, "회원");

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> orgMemberService.linkMemberToUser(memberId, 20L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_ALREADY_LINKED);
        }
    }

    @Nested
    @DisplayName("멤버 삭제 테스트")
    class DeleteMemberTest {

        @Test
        @DisplayName("삭제 성공 (Soft Delete)")
        void deleteMember_success() {
            // given
            Long memberId = 1L;
            OrgMember member = createOrgMember(memberId, 1L, null, "홍길동");

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.of(member));

            // when
            orgMemberService.deleteMember(memberId);

            // then
            assertThat(member.isDeleted()).isTrue();
        }
    }

    private OrgMember createOrgMember(Long id, Long sessionId, Long userId, String nickname) {
        OrgMember member = OrgMember.builder()
                .sessionId(sessionId)
                .userId(userId)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
