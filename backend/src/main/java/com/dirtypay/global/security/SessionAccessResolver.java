package com.dirtypay.global.security;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
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
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import com.dirtypay.global.security.annotation.SessionAccess.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리소스 ID와 타입으로부터 소유 Session을 조회하는 Resolver.
 *
 * <p>리소스 타입에 따라 엔티티 체인을 따라가며 최종 {@link Session}을 반환한다.
 * {@link SessionAccessAspect}에서 인증/인가 판단과 분리된 엔티티 조회 책임을 담당한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SessionAccessResolver {

    private final SessionRepository sessionRepository;
    private final NodeRepository nodeRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final RoundRepository roundRepository;
    private final OrderRepository orderRepository;
    private final RoundGroupRepository roundGroupRepository;

    /**
     * 리소스 ID와 타입으로부터 소유 Session을 조회한다.
     *
     * @param resourceId 리소스 ID
     * @param type       리소스 타입
     * @return 해당 리소스가 속한 {@link Session}
     * @throws EntityNotFoundException 체인 조회 중 엔티티를 찾을 수 없는 경우
     */
    public Session resolve(Long resourceId, ResourceType type) {
        return switch (type) {
            case SESSION -> this.findSession(resourceId);
            case NODE -> {
                Node node = this.findNode(resourceId);
                yield this.findSession(node.getSessionId());
            }
            case MEMBER -> {
                OrgMember member = this.findOrgMember(resourceId);
                yield this.findSession(member.getSessionId());
            }
            case ROUND -> {
                Round round = this.findRound(resourceId);
                yield this.findSession(round.getSessionId());
            }
            case ORDER -> {
                Order order = this.findOrder(resourceId);
                Round round = this.findRound(order.getRoundId());
                yield this.findSession(round.getSessionId());
            }
            case GROUP -> {
                RoundGroup group = this.findRoundGroup(resourceId);
                Round round = this.findRound(group.getRoundId());
                yield this.findSession(round.getSessionId());
            }
        };
    }

    private Session findSession(Long sessionId) {
        return this.sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
    }

    private Node findNode(Long nodeId) {
        return this.nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.NODE_NOT_FOUND));
    }

    private OrgMember findOrgMember(Long memberId) {
        return this.orgMemberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Round findRound(Long roundId) {
        return this.roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_NOT_FOUND));
    }

    private Order findOrder(Long orderId) {
        return this.orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    private RoundGroup findRoundGroup(Long groupId) {
        return this.roundGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND));
    }
}
