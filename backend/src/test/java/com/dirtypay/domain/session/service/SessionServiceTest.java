package com.dirtypay.domain.session.service;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.dto.request.SessionCreateRequest;
import com.dirtypay.domain.session.dto.request.SessionUpdateRequest;
import com.dirtypay.domain.session.dto.response.SessionResponse;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import com.dirtypay.domain.session.repository.SessionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @InjectMocks
    private SessionService sessionService;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private OrderRepository orderRepository;

    @Nested
    @DisplayName("세션 생성 테스트")
    class CreateSessionTest {

        @Test
        @DisplayName("세션 생성 성공 - 소유자 OrgMember가 자동 생성된다")
        void createSession_success() {
            // given
            Long ownerId = 1L;
            SessionCreateRequest request = new SessionCreateRequest();
            ReflectionTestUtils.setField(request, "title", "테스트 세션");
            ReflectionTestUtils.setField(request, "description", "설명");
            ReflectionTestUtils.setField(request, "startDate", LocalDate.of(2026, 2, 1));
            ReflectionTestUtils.setField(request, "endDate", LocalDate.of(2026, 2, 28));

            Session savedSession = createSession(1L, "테스트 세션", "설명", ownerId);

            Member owner = Member.builder()
                    .email("owner@test.com")
                    .password("password")
                    .name("소유자")
                    .build();
            ReflectionTestUtils.setField(owner, "id", ownerId);

            given(sessionRepository.save(any(Session.class))).willReturn(savedSession);
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(orgMemberRepository.save(any(OrgMember.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            SessionResponse response = sessionService.createSession(request, ownerId);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("테스트 세션");
            assertThat(response.getStatus()).isEqualTo(SessionStatus.ACTIVE);
            assertThat(response.getOwnerId()).isEqualTo(ownerId);

            verify(orgMemberRepository).save(any(OrgMember.class));
        }

        @Test
        @DisplayName("세션 생성 시 소유자 OrgMember에 sessionId가 설정된다")
        void createSession_ownerOrgMemberHasSessionId() {
            // given
            Long ownerId = 1L;
            SessionCreateRequest request = new SessionCreateRequest();
            ReflectionTestUtils.setField(request, "title", "테스트 세션");

            Session savedSession = createSession(1L, "테스트 세션", null, ownerId);

            Member owner = Member.builder()
                    .email("owner@test.com")
                    .password("password")
                    .name("소유자")
                    .build();
            ReflectionTestUtils.setField(owner, "id", ownerId);

            java.util.List<OrgMember> capturedMembers = new java.util.ArrayList<>();

            given(sessionRepository.save(any(Session.class))).willReturn(savedSession);
            given(memberRepository.findById(ownerId)).willReturn(Optional.of(owner));
            given(orgMemberRepository.save(any(OrgMember.class))).willAnswer(invocation -> {
                OrgMember member = invocation.getArgument(0);
                capturedMembers.add(member);
                return member;
            });

            // when
            sessionService.createSession(request, ownerId);

            // then
            assertThat(capturedMembers).hasSize(1);
            assertThat(capturedMembers.get(0).getSessionId()).isEqualTo(1L);
            assertThat(capturedMembers.get(0).getUserId()).isEqualTo(ownerId);
        }
    }

    @Nested
    @DisplayName("세션 목록 조회 테스트")
    class GetSessionsTest {

        @Test
        @DisplayName("소유자의 세션 목록 조회 성공")
        void getSessions_success() {
            // given
            Long ownerId = 1L;
            List<Session> sessions = List.of(
                    createSession(1L, "세션 1", null, ownerId),
                    createSession(2L, "세션 2", null, ownerId)
            );

            given(sessionRepository.findByMemberUserIdAndStatus(ownerId, SessionStatus.ACTIVE))
                    .willReturn(sessions);
            given(orgMemberRepository.countBySessionId(anyLong())).willReturn(3L);
            given(roundRepository.countBySessionId(anyLong())).willReturn(2L);
            given(orderRepository.sumTotalPriceBySessionId(anyLong())).willReturn(BigDecimal.valueOf(15000));

            // when
            List<SessionResponse> response = sessionService.getSessions(ownerId);

            // then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).getTitle()).isEqualTo("세션 1");
            assertThat(response.get(1).getTitle()).isEqualTo("세션 2");
            assertThat(response.get(0).getMemberCount()).isEqualTo(3L);
            assertThat(response.get(0).getRoundCount()).isEqualTo(2L);
            assertThat(response.get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        }

        @Test
        @DisplayName("세션이 없으면 빈 목록 반환")
        void getSessions_empty() {
            // given
            Long ownerId = 1L;

            given(sessionRepository.findByMemberUserIdAndStatus(ownerId, SessionStatus.ACTIVE))
                    .willReturn(List.of());

            // when
            List<SessionResponse> response = sessionService.getSessions(ownerId);

            // then
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("세션 상세 조회 테스트")
    class GetSessionTest {

        @Test
        @DisplayName("세션 상세 조회 성공")
        void getSession_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "테스트 세션", "설명", 1L);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(orgMemberRepository.countBySessionId(sessionId)).willReturn(5L);
            given(roundRepository.countBySessionId(sessionId)).willReturn(3L);
            given(orderRepository.sumTotalPriceBySessionId(sessionId)).willReturn(BigDecimal.valueOf(25000));

            // when
            SessionResponse response = sessionService.getSession(sessionId);

            // then
            assertThat(response.getId()).isEqualTo(sessionId);
            assertThat(response.getTitle()).isEqualTo("테스트 세션");
            assertThat(response.getMemberCount()).isEqualTo(5L);
            assertThat(response.getRoundCount()).isEqualTo(3L);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        }

        @Test
        @DisplayName("존재하지 않는 세션 조회 시 예외 발생")
        void getSession_notFound() {
            // given
            Long sessionId = 999L;

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionService.getSession(sessionId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("세션 수정 테스트")
    class UpdateSessionTest {

        @Test
        @DisplayName("세션 수정 성공")
        void updateSession_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "원래 제목", "원래 설명", 1L);

            SessionUpdateRequest request = new SessionUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "새 제목");
            ReflectionTestUtils.setField(request, "description", "새 설명");
            ReflectionTestUtils.setField(request, "startDate", LocalDate.of(2026, 3, 1));
            ReflectionTestUtils.setField(request, "endDate", LocalDate.of(2026, 3, 31));

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            SessionResponse response = sessionService.updateSession(sessionId, request);

            // then
            assertThat(response.getTitle()).isEqualTo("새 제목");
            assertThat(response.getDescription()).isEqualTo("새 설명");
        }
    }

    @Nested
    @DisplayName("세션 완료(Archive) 테스트")
    class ArchiveSessionTest {

        @Test
        @DisplayName("활성 세션 완료 성공")
        void archiveSession_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "테스트 세션", "설명", 1L);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundRepository.findBySessionId(sessionId)).willReturn(List.of());
            given(orgMemberRepository.countBySessionId(sessionId)).willReturn(3L);
            given(roundRepository.countBySessionId(sessionId)).willReturn(2L);
            given(orderRepository.sumTotalPriceBySessionId(sessionId)).willReturn(BigDecimal.valueOf(15000));

            // when
            SessionResponse response = sessionService.archiveSession(sessionId);

            // then
            assertThat(response.getId()).isEqualTo(sessionId);
            assertThat(response.getStatus()).isEqualTo(SessionStatus.ARCHIVED);
        }

        @Test
        @DisplayName("이미 완료된 세션 완료 시 예외 발생")
        void archiveSession_alreadyArchived() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "테스트 세션", "설명", 1L);
            session.archive(); // 미리 완료 상태로 변경

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> sessionService.archiveSession(sessionId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("세션 삭제 테스트")
    class DeleteSessionTest {

        @Test
        @DisplayName("세션 삭제 성공 (Soft Delete)")
        void deleteSession_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "세션", null, 1L);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            sessionService.deleteSession(sessionId);

            // then
            assertThat(session.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 세션 삭제 시 예외 발생")
        void deleteSession_notFound() {
            // given
            Long sessionId = 999L;

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionService.deleteSession(sessionId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ARCHIVED 세션 수정 방지 테스트")
    class ArchivedSessionGuardTest {

        @Test
        @DisplayName("ARCHIVED 세션 수정 시 예외 발생")
        void updateSession_archived_throwsException() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "세션", "설명", 1L);
            session.archive();

            SessionUpdateRequest request = new SessionUpdateRequest();
            ReflectionTestUtils.setField(request, "title", "새 제목");
            ReflectionTestUtils.setField(request, "description", "새 설명");

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> sessionService.updateSession(sessionId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ARCHIVED 세션도 삭제 성공")
        void deleteSession_archived_success() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "세션", null, 1L);
            session.archive();

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            sessionService.deleteSession(sessionId);

            // then
            assertThat(session.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("archiveSession 시 하위 OPEN 라운드가 CLOSED로 변경된다")
        void archiveSession_cascadeClosesOpenRounds() {
            // given
            Long sessionId = 1L;
            Session session = createSession(sessionId, "세션", null, 1L);

            Round openRound = createRound(1L, sessionId, RoundStatus.OPEN);
            Round closedRound = createRound(2L, sessionId, RoundStatus.CLOSED);

            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));
            given(roundRepository.findBySessionId(sessionId))
                    .willReturn(List.of(openRound, closedRound));
            given(orgMemberRepository.countBySessionId(sessionId)).willReturn(0L);
            given(roundRepository.countBySessionId(sessionId)).willReturn(2L);
            given(orderRepository.sumTotalPriceBySessionId(sessionId)).willReturn(BigDecimal.ZERO);

            // when
            sessionService.archiveSession(sessionId);

            // then
            assertThat(openRound.isClosed()).isTrue();
            assertThat(closedRound.isClosed()).isTrue();
        }
    }

    private Round createRound(Long id, Long sessionId, RoundStatus status) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title("라운드")
                .status(status)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private Session createSession(Long id, String title, String description, Long ownerId) {
        Session session = Session.builder()
                .title(title)
                .description(description)
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
