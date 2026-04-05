package com.dirtypay.global.security;

import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.Node;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.NodeRepository;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.exception.EntityNotFoundException;
import com.dirtypay.global.security.annotation.SessionAccess.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SessionAccessResolverTest {

    @InjectMocks
    private SessionAccessResolver sessionAccessResolver;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private OrderRepository orderRepository;

    private static final Long OWNER_ID = 1L;

    @Nested
    @DisplayName("SESSION 타입 조회")
    class SessionTypeTest {

        @Test
        @DisplayName("Session 직접 조회 성공")
        void resolve_session_success() {
            // given
            Long sessionId = 10L;
            Session session = createSession(sessionId, OWNER_ID);
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            Session result = sessionAccessResolver.resolve(sessionId, ResourceType.SESSION);

            // then
            assertThat(result.getId()).isEqualTo(sessionId);
            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        }

        @Test
        @DisplayName("Session 미존재 시 EntityNotFoundException 발생")
        void resolve_session_notFound() {
            // given
            Long sessionId = 10L;
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionAccessResolver.resolve(sessionId, ResourceType.SESSION))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("NODE 타입 조회")
    class NodeTypeTest {

        @Test
        @DisplayName("Node → Session 체인 조회 성공")
        void resolve_node_success() {
            // given
            Long nodeId = 20L;
            Long sessionId = 10L;
            Node node = createNode(nodeId, sessionId);
            Session session = createSession(sessionId, OWNER_ID);

            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.of(node));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            Session result = sessionAccessResolver.resolve(nodeId, ResourceType.NODE);

            // then
            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("Node 미존재 시 EntityNotFoundException 발생")
        void resolve_node_notFound() {
            // given
            Long nodeId = 20L;
            given(nodeRepository.findById(nodeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionAccessResolver.resolve(nodeId, ResourceType.NODE))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("MEMBER 타입 조회")
    class MemberTypeTest {

        @Test
        @DisplayName("Member → Session 직접 조회 성공")
        void resolve_member_success() {
            // given
            Long memberId = 30L;
            Long sessionId = 10L;
            OrgMember orgMember = createOrgMember(memberId, sessionId);
            Session session = createSession(sessionId, OWNER_ID);

            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.of(orgMember));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            Session result = sessionAccessResolver.resolve(memberId, ResourceType.MEMBER);

            // then
            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("Member 미존재 시 EntityNotFoundException 발생")
        void resolve_member_notFound() {
            // given
            Long memberId = 30L;
            given(orgMemberRepository.findById(memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionAccessResolver.resolve(memberId, ResourceType.MEMBER))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ROUND 타입 조회")
    class RoundTypeTest {

        @Test
        @DisplayName("Round → Session 체인 조회 성공")
        void resolve_round_success() {
            // given
            Long roundId = 40L;
            Long sessionId = 10L;
            Round round = createRound(roundId, sessionId);
            Session session = createSession(sessionId, OWNER_ID);

            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            Session result = sessionAccessResolver.resolve(roundId, ResourceType.ROUND);

            // then
            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("Round 미존재 시 EntityNotFoundException 발생")
        void resolve_round_notFound() {
            // given
            Long roundId = 40L;
            given(roundRepository.findById(roundId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionAccessResolver.resolve(roundId, ResourceType.ROUND))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ORDER 타입 조회")
    class OrderTypeTest {

        @Test
        @DisplayName("Order → Round → Session 체인 조회 성공")
        void resolve_order_success() {
            // given
            Long orderId = 60L;
            Long roundId = 40L;
            Long sessionId = 10L;
            Order order = createOrder(orderId, roundId);
            Round round = createRound(roundId, sessionId);
            Session session = createSession(sessionId, OWNER_ID);

            given(orderRepository.findById(orderId))
                    .willReturn(Optional.of(order));
            given(roundRepository.findById(roundId))
                    .willReturn(Optional.of(round));
            given(sessionRepository.findById(sessionId))
                    .willReturn(Optional.of(session));

            // when
            Session result = sessionAccessResolver.resolve(orderId, ResourceType.ORDER);

            // then
            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("Order 미존재 시 EntityNotFoundException 발생")
        void resolve_order_notFound() {
            // given
            Long orderId = 60L;
            given(orderRepository.findById(orderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sessionAccessResolver.resolve(orderId, ResourceType.ORDER))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // === Helper Methods ===

    private Session createSession(Long id, Long ownerId) {
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(ownerId)
                .build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private Node createNode(Long id, Long sessionId) {
        Node node = Node.builder()
                .sessionId(sessionId)
                .name("테스트 노드")
                .depth(0)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(node, "id", id);
        return node;
    }

    private OrgMember createOrgMember(Long id, Long sessionId) {
        OrgMember member = OrgMember.builder()
                .sessionId(sessionId)
                .nickname("테스트 멤버")
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Round createRound(Long id, Long sessionId) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title("테스트 라운드")
                .sortOrder(1)
                .build();
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private Order createOrder(Long id, Long roundId) {
        Order order = Order.builder()
                .roundId(roundId)
                .menuId(1L)
                .quantity(1)
                .totalPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }
}
