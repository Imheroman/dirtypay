package com.dirtypay.domain.order.service;

import com.dirtypay.domain.order.dto.request.OrderCreateRequest;
import com.dirtypay.domain.order.dto.request.OrderUpdateRequest;
import com.dirtypay.domain.order.dto.response.OrderDetailResponse;
import com.dirtypay.domain.order.dto.response.OrderResponse;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 서비스.
 *
 * <p>주문의 CRUD 및 조회 비즈니스 로직을 처리한다.
 * N+1 문제를 방지하기 위해 수동 배치 fetch를 사용한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupMemberRepository roundGroupMemberRepository;
    private final OrgMemberService orgMemberService;

    /**
     * 새로운 주문을 생성한다.
     *
     * <p>OPEN 상태 검증 후 RoundGroup을 조회하여 라운드 일치 여부를 검증한다.
     * totalPrice를 계산하고 멤버별 OrderDetail을 일괄 생성한다.</p>
     *
     * @param roundId 라운드 ID
     * @param request 주문 생성 요청
     * @return 생성된 주문 응답
     * @throws BusinessException groupId가 라운드와 불일치할 경우 ORDER_GROUP_ROUND_MISMATCH
     */
    @Transactional
    public OrderResponse createOrder(Long roundId, OrderCreateRequest request) {
        Round round = this.findRoundById(roundId);
        round.verifyOpen();

        RoundGroup group = this.findGroupById(request.getGroupId());
        this.verifyGroupRound(group, round);

        StoreMenu storeMenu = this.findMenuById(request.getMenuId());
        Map<Long, StoreMenu> menuMap = Map.of(storeMenu.getId(), storeMenu);

        return this.createSingleOrder(roundId, request, menuMap, group);
    }

    /**
     * 주문을 일괄 생성한다.
     *
     * <p>OPEN 상태 검증 후 RoundGroup과 메뉴를 배치 조회하여 N+1 문제를 방지한다.
     * 각 request의 groupId가 라운드와 일치하는지 검증한 후 순차 생성한다.</p>
     *
     * @param roundId  라운드 ID
     * @param requests 주문 생성 요청 목록
     * @return 생성된 주문 응답 목록
     * @throws BusinessException groupId가 라운드와 불일치할 경우 ORDER_GROUP_ROUND_MISMATCH
     */
    @Transactional
    public List<OrderResponse> createOrders(Long roundId, List<OrderCreateRequest> requests) {
        Round round = this.findRoundById(roundId);
        round.verifyOpen();

        List<Long> groupIds = requests.stream()
                .map(OrderCreateRequest::getGroupId)
                .distinct()
                .toList();
        Map<Long, RoundGroup> groupMap = this.roundGroupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(RoundGroup::getId, Function.identity()));
        groupMap.values().forEach(group -> this.verifyGroupRound(group, round));

        List<Long> menuIds = requests.stream()
                .map(OrderCreateRequest::getMenuId)
                .distinct()
                .toList();
        Map<Long, StoreMenu> menuMap = this.storeMenuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(StoreMenu::getId, Function.identity()));

        return requests.stream()
                .map(request -> {
                    RoundGroup group = groupMap.get(request.getGroupId());
                    if (group == null) {
                        throw new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND);
                    }
                    return this.createSingleOrder(roundId, request, menuMap, group);
                })
                .toList();
    }

    /**
     * 라운드의 주문 목록을 조회한다.
     *
     * <p>orgMemberId가 지정되면 해당 멤버가 참여한 주문만 필터링한다.
     * groupId가 지정되면 해당 그룹의 주문만 필터링한다.</p>
     *
     * @param roundId     라운드 ID
     * @param orgMemberId 멤버 ID (nullable)
     * @param groupId     그룹 ID (nullable)
     * @return 주문 목록
     */
    public List<OrderResponse> getOrdersByRound(Long roundId, Long orgMemberId, Long groupId) {
        List<Order> orders = this.orderRepository.findByRoundId(roundId);

        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderDetail> allDetails = this.orderDetailRepository
                .findByOrderIdIn(orderIds);

        if (orgMemberId != null) {
            List<Long> filteredOrderIds = allDetails.stream()
                    .filter(d -> d.getOrgMemberId().equals(orgMemberId))
                    .map(OrderDetail::getOrderId)
                    .distinct()
                    .toList();

            orders = orders.stream()
                    .filter(o -> filteredOrderIds.contains(o.getId()))
                    .toList();
        }

        if (groupId != null) {
            orders = orders.stream()
                    .filter(o -> groupId.equals(o.getGroupId()))
                    .toList();
        }

        Map<Long, List<OrderDetail>> detailsByOrderId = allDetails.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        List<Long> memberIds = allDetails.stream()
                .map(OrderDetail::getOrgMemberId).distinct().toList();
        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(memberIds);

        return orders.stream()
                .map(order -> {
                    List<OrderDetail> orderDetails = detailsByOrderId
                            .getOrDefault(order.getId(), List.of());
                    List<OrderDetailResponse> detailResponses = orderDetails.stream()
                            .map(d -> OrderDetailResponse.from(d,
                                    nicknameMap.getOrDefault(d.getOrgMemberId(), "")))
                            .toList();
                    return OrderResponse.from(order, detailResponses);
                })
                .toList();
    }

    /**
     * 주문 수량을 수정한다.
     *
     * @param orderId 주문 ID
     * @param request 수정 요청
     * @return 수정된 주문 응답
     */
    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderUpdateRequest request) {
        Order order = this.findOrderById(orderId);
        Round round = this.findRoundById(order.getRoundId());
        round.verifyOpen();

        order.updateQuantity(request.getQuantity());

        List<OrderDetail> details = this.orderDetailRepository
                .findByOrderId(orderId);
        List<Long> memberIds = details.stream()
                .map(OrderDetail::getOrgMemberId).toList();
        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(memberIds);

        List<OrderDetailResponse> detailResponses = details.stream()
                .map(d -> OrderDetailResponse.from(d, nicknameMap.getOrDefault(d.getOrgMemberId(), "")))
                .toList();

        return OrderResponse.from(order, detailResponses);
    }

    /**
     * 주문을 삭제한다. (Soft Delete)
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = this.findOrderById(orderId);
        Round round = this.findRoundById(order.getRoundId());
        round.verifyOpen();

        order.delete();
    }

    /**
     * 단일 주문을 생성한다.
     *
     * <p>그룹 계층 기반으로 참여자를 결정한다.
     * memberIds가 null이면 해당 그룹 + 하위 그룹의 전체 멤버를 자동 포함하고,
     * 제공되면 허용 범위(그룹 + 하위 그룹 멤버) 내인지 검증한다.</p>
     *
     * @param roundId 라운드 ID
     * @param request 주문 생성 요청
     * @param menuMap 메뉴 맵
     * @param group   주문 대상 그룹
     * @return 생성된 주문 응답
     * @throws BusinessException memberIds에 그룹 범위 밖 멤버가 포함된 경우 ORDER_MEMBER_NOT_IN_GROUP
     */
    private OrderResponse createSingleOrder(Long roundId, OrderCreateRequest request,
                                               Map<Long, StoreMenu> menuMap, RoundGroup group) {
        StoreMenu storeMenu = menuMap.get(request.getMenuId());
        if (storeMenu == null) {
            throw new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND);
        }

        // 그룹 계층 기반 허용 멤버 수집
        List<Long> allowedMemberIds = this.collectGroupHierarchyMemberIds(roundId, group.getId());

        // 참여자 결정: memberIds 미제공 시 전체 허용 멤버, 제공 시 검증 후 사용
        List<Long> memberIds;
        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            memberIds = allowedMemberIds;
        } else {
            Set<Long> allowedSet = new HashSet<>(allowedMemberIds);
            List<Long> invalidIds = request.getMemberIds().stream()
                    .filter(id -> !allowedSet.contains(id))
                    .toList();
            if (!invalidIds.isEmpty()) {
                throw new BusinessException(ErrorCode.ORDER_MEMBER_NOT_IN_GROUP);
            }
            memberIds = request.getMemberIds().stream().distinct().toList();
        }

        BigDecimal totalPrice = storeMenu.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .roundId(roundId)
                .menuId(request.getMenuId())
                .menuName(storeMenu.getName())
                .menuPrice(storeMenu.getPrice())
                .menuCategory(storeMenu.getCategory())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .groupId(group.getId())
                .groupName(group.getName())
                .build();

        Order savedOrder = this.orderRepository.save(order);

        List<OrderDetail> details = memberIds.stream()
                .map(memberId -> OrderDetail.builder()
                        .orderId(savedOrder.getId())
                        .orgMemberId(memberId)
                        .build())
                .toList();

        List<OrderDetail> savedDetails = this.orderDetailRepository.saveAll(details);

        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(memberIds);

        List<OrderDetailResponse> detailResponses = savedDetails.stream()
                .map(d -> OrderDetailResponse.from(d, nicknameMap.getOrDefault(d.getOrgMemberId(), "")))
                .toList();

        return OrderResponse.from(savedOrder, detailResponses);
    }

    private Order findOrderById(Long orderId) {
        return this.orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    private RoundGroup findGroupById(Long groupId) {
        return this.roundGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND));
    }

    /**
     * 그룹이 해당 라운드에 속하는지 검증한다.
     *
     * @param group 라운드 그룹
     * @param round 라운드
     * @throws BusinessException 그룹이 라운드에 속하지 않는 경우
     */
    private void verifyGroupRound(RoundGroup group, Round round) {
        if (!group.getRoundId().equals(round.getId())) {
            throw new BusinessException(ErrorCode.ORDER_GROUP_ROUND_MISMATCH);
        }
    }

    /**
     * 그룹 + 하위 그룹의 전체 멤버 ID를 수집한다.
     *
     * <p>라운드의 전체 그룹을 조회하여 인메모리 트리를 빌드하고,
     * 대상 그룹의 하위 그룹을 재귀적으로 수집한 뒤 멤버를 일괄 조회한다.</p>
     *
     * @param roundId 라운드 ID
     * @param groupId 대상 그룹 ID
     * @return 해당 그룹 + 하위 그룹의 전체 멤버 ID 목록 (중복 제거)
     */
    private List<Long> collectGroupHierarchyMemberIds(Long roundId, Long groupId) {
        List<RoundGroup> allGroups = this.roundGroupRepository.findByRoundId(roundId);
        Map<Long, List<RoundGroup>> childrenMap = allGroups.stream()
                .filter(g -> g.getParentGroupId() != null)
                .collect(Collectors.groupingBy(RoundGroup::getParentGroupId));

        List<Long> targetGroupIds = new ArrayList<>();
        this.collectDescendantGroupIds(groupId, targetGroupIds, childrenMap);

        return this.roundGroupMemberRepository.findByGroupIdIn(targetGroupIds).stream()
                .map(RoundGroupMember::getOrgMemberId)
                .distinct()
                .toList();
    }

    /**
     * 대상 그룹 ID + 모든 하위 그룹 ID를 재귀적으로 수집한다.
     *
     * @param groupId     현재 그룹 ID
     * @param collected   수집된 그룹 ID 목록
     * @param childrenMap parentGroupId → 자식 그룹 목록 맵
     */
    private void collectDescendantGroupIds(Long groupId, List<Long> collected,
                                            Map<Long, List<RoundGroup>> childrenMap) {
        collected.add(groupId);
        List<RoundGroup> children = childrenMap.getOrDefault(groupId, List.of());
        for (RoundGroup child : children) {
            this.collectDescendantGroupIds(child.getId(), collected, childrenMap);
        }
    }

    private StoreMenu findMenuById(Long menuId) {
        return this.storeMenuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));
    }

    private Round findRoundById(Long roundId) {
        return this.roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_NOT_FOUND));
    }

}
