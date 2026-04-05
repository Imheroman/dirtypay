package com.dirtypay.global.security;

import com.dirtypay.domain.auth.security.UserPrincipal;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.security.annotation.SessionAccess;
import com.dirtypay.global.security.annotation.SessionAccess.AccessLevel;
import com.dirtypay.global.security.annotation.SessionAccess.ResourceType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SessionAccessAspectTest {

    @InjectMocks
    private SessionAccessAspect sessionAccessAspect;

    @Mock
    private SessionAccessResolver sessionAccessResolver;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long MEMBER_USER_ID = 3L;

    @BeforeEach
    void setUpSecurityContext() {
        setAuthenticatedUser(OWNER_ID);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("SESSION 타입 검증")
    class SessionTypeTest {

        @Test
        @DisplayName("소유자 접근 시 통과")
        void session_ownerAccess_passes() {
            // given
            Long sessionId = 10L;
            Session session = createSession(sessionId, OWNER_ID);
            given(sessionAccessResolver.resolve(sessionId, ResourceType.SESSION))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"sessionId"}, new Object[]{sessionId});
            SessionAccess annotation = mockAnnotation("sessionId", ResourceType.SESSION);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비소유자 접근 시 BusinessException 발생")
        void session_nonOwnerAccess_throwsException() {
            // given
            Long sessionId = 10L;
            Session session = createSession(sessionId, OTHER_USER_ID);
            given(sessionAccessResolver.resolve(sessionId, ResourceType.SESSION))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"sessionId"}, new Object[]{sessionId});
            SessionAccess annotation = mockAnnotation("sessionId", ResourceType.SESSION, AccessLevel.OWNER);

            // when & then
            assertThatThrownBy(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("NODE 타입 검증")
    class NodeTypeTest {

        @Test
        @DisplayName("Node → Session 체인 검증 통과")
        void node_ownerAccess_passes() {
            // given
            Long nodeId = 20L;
            Session session = createSession(10L, OWNER_ID);
            given(sessionAccessResolver.resolve(nodeId, ResourceType.NODE))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"nodeId"}, new Object[]{nodeId});
            SessionAccess annotation = mockAnnotation("nodeId", ResourceType.NODE);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MEMBER 타입 검증")
    class MemberTypeTest {

        @Test
        @DisplayName("Member → Node → Session 체인 검증 통과")
        void member_ownerAccess_passes() {
            // given
            Long memberId = 30L;
            Session session = createSession(10L, OWNER_ID);
            given(sessionAccessResolver.resolve(memberId, ResourceType.MEMBER))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"memberId"}, new Object[]{memberId});
            SessionAccess annotation = mockAnnotation("memberId", ResourceType.MEMBER);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ROUND 타입 검증")
    class RoundTypeTest {

        @Test
        @DisplayName("Round → Session 체인 검증 통과")
        void round_ownerAccess_passes() {
            // given
            Long roundId = 40L;
            Session session = createSession(10L, OWNER_ID);
            given(sessionAccessResolver.resolve(roundId, ResourceType.ROUND))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"roundId"}, new Object[]{roundId});
            SessionAccess annotation = mockAnnotation("roundId", ResourceType.ROUND);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ORDER 타입 검증")
    class OrderTypeTest {

        @Test
        @DisplayName("Order → Round → Session 체인 검증 통과")
        void order_ownerAccess_passes() {
            // given
            Long orderId = 60L;
            Session session = createSession(10L, OWNER_ID);
            given(sessionAccessResolver.resolve(orderId, ResourceType.ORDER))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"orderId"}, new Object[]{orderId});
            SessionAccess annotation = mockAnnotation("orderId", ResourceType.ORDER);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("MEMBER 레벨 접근 검증")
    class MemberLevelTest {

        @Test
        @DisplayName("소유자는 MEMBER 레벨에서도 통과")
        void memberLevel_ownerAccess_passes() {
            // given
            Long roundId = 40L;
            Session session = createSession(10L, OWNER_ID);
            given(sessionAccessResolver.resolve(roundId, ResourceType.ROUND))
                    .willReturn(session);

            JoinPoint joinPoint = mockJoinPoint(new String[]{"roundId"}, new Object[]{roundId});
            SessionAccess annotation = mockAnnotation("roundId", ResourceType.ROUND);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("세션 멤버는 MEMBER 레벨에서 통과")
        void memberLevel_memberAccess_passes() {
            // given
            setAuthenticatedUser(MEMBER_USER_ID);
            Long roundId = 40L;
            Long sessionId = 10L;
            Session session = createSession(sessionId, OTHER_USER_ID);
            given(sessionAccessResolver.resolve(roundId, ResourceType.ROUND))
                    .willReturn(session);

            OrgMember orgMember = OrgMember.builder()
                    .sessionId(1L)
                    .nickname("멤버")
                    .build();
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, MEMBER_USER_ID))
                    .willReturn(Optional.of(orgMember));

            JoinPoint joinPoint = mockJoinPoint(new String[]{"roundId"}, new Object[]{roundId});
            SessionAccess annotation = mockAnnotation("roundId", ResourceType.ROUND, AccessLevel.MEMBER);

            // when & then
            assertThatCode(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비멤버는 MEMBER 레벨에서 차단")
        void memberLevel_nonMemberAccess_throwsException() {
            // given
            setAuthenticatedUser(MEMBER_USER_ID);
            Long roundId = 40L;
            Long sessionId = 10L;
            Session session = createSession(sessionId, OTHER_USER_ID);
            given(sessionAccessResolver.resolve(roundId, ResourceType.ROUND))
                    .willReturn(session);
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, MEMBER_USER_ID))
                    .willReturn(Optional.empty());

            JoinPoint joinPoint = mockJoinPoint(new String[]{"roundId"}, new Object[]{roundId});
            SessionAccess annotation = mockAnnotation("roundId", ResourceType.ROUND, AccessLevel.MEMBER);

            // when & then
            assertThatThrownBy(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("인증 검증")
    class AuthenticationTest {

        @Test
        @DisplayName("인증되지 않은 사용자 접근 시 BusinessException 발생")
        void unauthenticated_throwsException() {
            // given
            SecurityContextHolder.clearContext();

            JoinPoint joinPoint = mock(JoinPoint.class);
            SessionAccess annotation = mock(SessionAccess.class);

            // when & then
            assertThatThrownBy(() -> sessionAccessAspect.verifySessionAccess(joinPoint, annotation))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // === Helper Methods ===

    private void setAuthenticatedUser(Long userId) {
        Member member = Member.builder()
                .email("test@test.com")
                .password("password")
                .name("테스트")
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", userId);
        UserPrincipal principal = UserPrincipal.from(member);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private JoinPoint mockJoinPoint(String[] parameterNames, Object[] args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getParameterNames()).willReturn(parameterNames);
        given(joinPoint.getArgs()).willReturn(args);
        return joinPoint;
    }

    private SessionAccess mockAnnotation(String value, ResourceType type) {
        SessionAccess annotation = mock(SessionAccess.class);
        given(annotation.value()).willReturn(value);
        given(annotation.type()).willReturn(type);
        return annotation;
    }

    private SessionAccess mockAnnotation(String value, ResourceType type, AccessLevel level) {
        SessionAccess annotation = mock(SessionAccess.class);
        given(annotation.value()).willReturn(value);
        given(annotation.type()).willReturn(type);
        given(annotation.level()).willReturn(level);
        return annotation;
    }

    private Session createSession(Long id, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
