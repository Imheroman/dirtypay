package com.dirtypay.domain.settlement.service;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.organization.service.OrgMemberService;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.domain.settlement.dto.response.MemberAmountResponse;
import com.dirtypay.domain.settlement.dto.response.MemberOrderDetail;
import com.dirtypay.domain.settlement.dto.response.MemberRoundDetail;
import com.dirtypay.domain.settlement.dto.response.MemberSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.NodeCategoryGroupResponse;
import com.dirtypay.domain.settlement.dto.response.NodeMemberCountResponse;
import com.dirtypay.domain.settlement.dto.response.NodeMenuSummaryResponse;
import com.dirtypay.domain.settlement.dto.response.NodeOrderHistoryResponse;
import com.dirtypay.domain.settlement.dto.response.NodeOrdersResponse;
import com.dirtypay.domain.settlement.dto.response.NodeSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.OrderGroupResponse;
import com.dirtypay.domain.settlement.dto.response.OrderMemberShareResponse;
import com.dirtypay.domain.settlement.dto.response.OrderSettlementItemResponse;
import com.dirtypay.domain.settlement.dto.response.OrderSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.RoundSettlementResponse;
import com.dirtypay.domain.settlement.dto.response.SessionSettlementResponse;
import com.dirtypay.domain.settlement.entity.SettlementPayment;
import com.dirtypay.domain.settlement.repository.SettlementPaymentRepository;
import com.dirtypay.domain.settlement.strategy.OwnerRemainderStrategy;
import com.dirtypay.domain.settlement.strategy.RandomRemainderStrategy;
import com.dirtypay.domain.settlement.strategy.RemainderStrategy;
import com.dirtypay.domain.settlement.strategy.RemainderStrategyType;
import com.dirtypay.domain.settlement.strategy.RoundUpRemainderStrategy;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import com.dirtypay.global.util.MoneyCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 정산 서비스.
 *
 * <p>라운드별, 세션별, 멤버별, 그룹별 정산을 계산한다.
 * 정산 시점의 그룹 계층 구조를 기준으로 균등 분배하며,
 * 나머지는 지정된 전략에 따라 처리한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final RoundRepository roundRepository;
    private final RoundParticipantRepository roundParticipantRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SessionRepository sessionRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgMemberService orgMemberService;
    private final SettlementPaymentRepository settlementPaymentRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupMemberRepository roundGroupMemberRepository;

    /**
     * 라운드 정산을 계산한다.
     *
     * @param roundId      라운드 ID
     * @param strategyType 나머지 분배 전략
     * @return 라운드 정산 응답
     */
    public RoundSettlementResponse calculateRoundSettlement(Long roundId,
                                                            RemainderStrategyType strategyType) {
        Round round = this.findRoundById(roundId);
        Session session = this.findSessionById(round.getSessionId());

        List<RoundParticipant> participants = this.roundParticipantRepository
                .findByRoundId(roundId);
        List<Order> orders = this.orderRepository.findByRoundId(roundId);

        Map<Long, List<Long>> groupMemberMap = this.buildGroupDescendantMemberMap(roundId);
        Map<Long, BigDecimal> memberAmounts = this.calculateMemberAmounts(
                orders, participants, groupMemberMap);

        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.applyRemainderStrategy(memberAmounts, totalAmount, participants,
                session.getOwnerId(), strategyType);

        List<Long> memberIds = participants.stream()
                .map(RoundParticipant::getOrgMemberId).toList();
        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(memberIds);

        Set<Long> excludedIds = participants.stream()
                .filter(RoundParticipant::isExcluded)
                .map(RoundParticipant::getOrgMemberId)
                .collect(Collectors.toSet());

        List<MemberAmountResponse> settlements = participants.stream()
                .map(p -> MemberAmountResponse.builder()
                        .orgMemberId(p.getOrgMemberId())
                        .nickname(nicknameMap.getOrDefault(p.getOrgMemberId(), ""))
                        .amount(memberAmounts.getOrDefault(p.getOrgMemberId(), BigDecimal.ZERO))
                        .isExcluded(excludedIds.contains(p.getOrgMemberId()))
                        .isPaid(false)
                        .paidAmount(BigDecimal.ZERO)
                        .remainingAmount(memberAmounts.getOrDefault(p.getOrgMemberId(), BigDecimal.ZERO))
                        .build())
                .toList();

        return RoundSettlementResponse.builder()
                .roundId(roundId)
                .totalAmount(totalAmount)
                .strategy(strategyType)
                .settlements(settlements)
                .build();
    }

    /**
     * 세션 정산을 계산한다.
     *
     * <p>세션에 속한 모든 라운드의 참여자·주문·그룹 데이터를 배치 조회하여
     * N+1 쿼리를 O(1) 수준으로 감소시킨다.</p>
     *
     * @param sessionId    세션 ID
     * @param strategyType 나머지 분배 전략
     * @return 세션 정산 응답
     */
    public SessionSettlementResponse calculateSessionSettlement(Long sessionId,
                                                                 RemainderStrategyType strategyType) {
        Session session = this.findSessionById(sessionId);

        List<Round> rounds = this.roundRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId);

        if (rounds.isEmpty()) {
            return SessionSettlementResponse.builder()
                    .sessionId(sessionId)
                    .totalAmount(BigDecimal.ZERO)
                    .strategy(strategyType)
                    .settlements(List.of())
                    .rounds(List.of())
                    .build();
        }

        List<Long> roundIds = rounds.stream().map(Round::getId).toList();

        // 배치 조회: 라운드별 반복 쿼리(N+1)를 단일 IN 쿼리로 대체
        Map<Long, List<RoundParticipant>> participantsByRoundId = this.roundParticipantRepository
                .findByRoundIdIn(roundIds).stream()
                .collect(Collectors.groupingBy(RoundParticipant::getRoundId));

        Map<Long, List<Order>> ordersByRoundId = this.orderRepository
                .findByRoundIdIn(roundIds).stream()
                .collect(Collectors.groupingBy(Order::getRoundId));

        List<RoundGroup> allGroups = this.roundGroupRepository.findByRoundIdIn(roundIds);
        Map<Long, List<RoundGroup>> groupsByRoundId = allGroups.stream()
                .collect(Collectors.groupingBy(RoundGroup::getRoundId));

        List<Long> allGroupIds = allGroups.stream().map(RoundGroup::getId).toList();
        List<RoundGroupMember> allGroupMembers = allGroupIds.isEmpty()
                ? List.of()
                : this.roundGroupMemberRepository.findByGroupIdIn(allGroupIds);
        Map<Long, List<RoundGroupMember>> groupMembersByGroupId = allGroupMembers.stream()
                .collect(Collectors.groupingBy(RoundGroupMember::getGroupId));

        // 전체 참여자 ID 수집 후 닉네임 일괄 조회
        Set<Long> allMemberIdSet = participantsByRoundId.values().stream()
                .flatMap(List::stream)
                .map(RoundParticipant::getOrgMemberId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = this.orgMemberService
                .getNicknameMap(new ArrayList<>(allMemberIdSet));

        // OrgMember 일괄 조회 (owner 인덱스 결정용)
        Map<Long, OrgMember> orgMemberMap = this.orgMemberRepository
                .findAllById(new ArrayList<>(allMemberIdSet)).stream()
                .collect(Collectors.toMap(OrgMember::getId, Function.identity()));

        List<RoundSettlementResponse> roundSettlements = rounds.stream()
                .map(round -> this.calculateRoundSettlementFromBatch(
                        round, session.getOwnerId(), strategyType,
                        participantsByRoundId.getOrDefault(round.getId(), List.of()),
                        ordersByRoundId.getOrDefault(round.getId(), List.of()),
                        groupsByRoundId.getOrDefault(round.getId(), List.of()),
                        groupMembersByGroupId,
                        nicknameMap,
                        orgMemberMap))
                .toList();

        Map<Long, BigDecimal> totalByMember = new HashMap<>();
        for (RoundSettlementResponse rs : roundSettlements) {
            for (MemberAmountResponse ma : rs.getSettlements()) {
                totalByMember.merge(ma.getOrgMemberId(), ma.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal totalAmount = roundSettlements.stream()
                .map(RoundSettlementResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, SettlementPayment> paymentMap = this.settlementPaymentRepository
                .findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(SettlementPayment::getOrgMemberId, sp -> sp));

        List<MemberAmountResponse> settlements = totalByMember.entrySet().stream()
                .map(entry -> {
                    SettlementPayment payment = paymentMap.get(entry.getKey());
                    BigDecimal paidAmount = payment != null ? payment.getPaidAmount() : BigDecimal.ZERO;
                    boolean isPaid = payment != null && payment.isPaid();
                    BigDecimal remainingAmount = entry.getValue().subtract(paidAmount).max(BigDecimal.ZERO);

                    return MemberAmountResponse.builder()
                            .orgMemberId(entry.getKey())
                            .nickname(nicknameMap.getOrDefault(entry.getKey(), ""))
                            .amount(entry.getValue())
                            .isExcluded(false)
                            .isPaid(isPaid)
                            .paidAmount(paidAmount)
                            .remainingAmount(remainingAmount)
                            .build();
                })
                .toList();

        return SessionSettlementResponse.builder()
                .sessionId(sessionId)
                .totalAmount(totalAmount)
                .strategy(strategyType)
                .settlements(settlements)
                .rounds(roundSettlements)
                .build();
    }

    /**
     * 멤버별 정산 상세를 계산한다.
     *
     * @param sessionId    세션 ID
     * @param orgMemberId  멤버 ID
     * @param strategyType 나머지 분배 전략
     * @return 멤버별 정산 응답
     */
    public MemberSettlementResponse calculateMemberSettlement(Long sessionId, Long orgMemberId,
                                                               RemainderStrategyType strategyType) {
        this.findSessionById(sessionId);

        List<Round> rounds = this.roundRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId);

        List<MemberRoundDetail> roundDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Round round : rounds) {
            List<Order> orders = this.orderRepository
                    .findByRoundId(round.getId());

            if (orders.isEmpty()) {
                continue;
            }

            Map<Long, List<Long>> groupMemberMap = this.buildGroupDescendantMemberMap(
                    round.getId());

            List<RoundParticipant> participants = this.roundParticipantRepository
                    .findByRoundId(round.getId());
            Set<Long> participantIds = participants.stream()
                    .map(RoundParticipant::getOrgMemberId)
                    .collect(Collectors.toSet());
            Set<Long> excludedMemberIds = participants.stream()
                    .filter(RoundParticipant::isExcluded)
                    .map(RoundParticipant::getOrgMemberId)
                    .collect(Collectors.toSet());

            List<MemberOrderDetail> memberOrders = new ArrayList<>();
            BigDecimal roundAmount = BigDecimal.ZERO;

            for (Order order : orders) {
                List<Long> eligibleIds = groupMemberMap
                        .getOrDefault(order.getGroupId(), List.of()).stream()
                        .filter(participantIds::contains)
                        .filter(id -> !excludedMemberIds.contains(id))
                        .toList();

                if (!eligibleIds.contains(orgMemberId)) {
                    continue;
                }

                MoneyCalculator.DivisionResult division = MoneyCalculator.divide(
                        order.getTotalPrice(), eligibleIds.size());
                // 나머지는 첫 번째 멤버(index 0)에게 할당하여 분배 합계 == 원금 보장
                boolean isFirstMember = eligibleIds.indexOf(orgMemberId) == 0;
                BigDecimal myShare = isFirstMember
                        ? division.quotient().add(division.remainder())
                        : division.quotient();

                memberOrders.add(MemberOrderDetail.builder()
                        .orderId(order.getId())
                        .menuName(order.getMenuName())
                        .quantity(order.getQuantity())
                        .totalPrice(order.getTotalPrice())
                        .myShare(myShare)
                        .build());

                roundAmount = roundAmount.add(myShare);
            }

            if (!memberOrders.isEmpty()) {
                roundDetails.add(MemberRoundDetail.builder()
                        .roundId(round.getId())
                        .amount(roundAmount)
                        .orders(memberOrders)
                        .build());

                totalAmount = totalAmount.add(roundAmount);
            }
        }

        SettlementPayment payment = this.settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(sessionId, orgMemberId)
                .orElse(null);

        BigDecimal paidAmount = payment != null ? payment.getPaidAmount() : BigDecimal.ZERO;
        boolean isPaid = payment != null && payment.isPaid();
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        return MemberSettlementResponse.builder()
                .orgMemberId(orgMemberId)
                .totalAmount(totalAmount)
                .isPaid(isPaid)
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .details(roundDetails)
                .build();
    }

    /**
     * 멤버의 정산 완료 상태를 업데이트한다.
     *
     * <p>납부 금액을 기록하고, 전액 납부 시 isPaid를 true로 설정한다.
     * 기존 기록이 없으면 새로 생성한다 (Upsert).</p>
     *
     * @param sessionId    세션 ID
     * @param orgMemberId  멤버 ID
     * @param paidAmount   납부 금액
     * @param strategyType 나머지 분배 전략
     * @return 멤버별 정산 응답
     */
    @Transactional
    public MemberSettlementResponse updateSettlementPayment(Long sessionId, Long orgMemberId,
                                                             BigDecimal paidAmount,
                                                             RemainderStrategyType strategyType) {
        MemberSettlementResponse memberSettlement = this.calculateMemberSettlement(
                sessionId, orgMemberId, strategyType);
        BigDecimal totalAmount = memberSettlement.getTotalAmount();

        if (paidAmount.compareTo(BigDecimal.ZERO) < 0 || paidAmount.compareTo(totalAmount) > 0) {
            throw new BusinessException(ErrorCode.SETTLEMENT_INVALID_PAID_AMOUNT);
        }

        SettlementPayment payment = this.settlementPaymentRepository
                .findBySessionIdAndOrgMemberId(sessionId, orgMemberId)
                .orElseGet(() -> this.settlementPaymentRepository.save(
                        SettlementPayment.builder()
                                .sessionId(sessionId)
                                .orgMemberId(orgMemberId)
                                .build()));

        payment.updatePayment(paidAmount, totalAmount);

        BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        return MemberSettlementResponse.builder()
                .orgMemberId(orgMemberId)
                .totalAmount(totalAmount)
                .isPaid(payment.isPaid())
                .paidAmount(payment.getPaidAmount())
                .remainingAmount(remainingAmount)
                .details(memberSettlement.getDetails())
                .build();
    }

    /**
     * 주문 중심 정산을 계산한다.
     *
     * <p>세션의 모든 주문을 카테고리별로 그룹화하고,
     * 각 주문에 참여하는 멤버의 원시 지분(shareRatio)을 기반으로 금액을 계산한다.
     * 나머지 분배 전략은 적용하지 않는다.</p>
     *
     * @param sessionId    세션 ID
     * @param strategyType 나머지 분배 전략 (응답에 표시용)
     * @return 주문 중심 정산 응답
     */
    public OrderSettlementResponse calculateOrderSettlement(Long sessionId,
                                                             RemainderStrategyType strategyType) {
        this.findSessionById(sessionId);

        List<Round> rounds = this.roundRepository
                .findBySessionIdOrderBySortOrderAsc(sessionId);

        // category → items 맵 (카테고리별 그룹핑)
        Map<String, List<OrderSettlementItemResponse>> categoryItemsMap = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Round round : rounds) {
            List<Order> orders = this.orderRepository.findByRoundId(round.getId());

            if (orders.isEmpty()) {
                continue;
            }

            List<RoundParticipant> participants = this.roundParticipantRepository
                    .findByRoundId(round.getId());

            Set<Long> excludedMemberIds = participants.stream()
                    .filter(RoundParticipant::isExcluded)
                    .map(RoundParticipant::getOrgMemberId)
                    .collect(Collectors.toSet());

            Set<Long> participantIds = participants.stream()
                    .map(RoundParticipant::getOrgMemberId)
                    .collect(Collectors.toSet());

            Map<Long, List<Long>> groupMemberMap = this.buildGroupDescendantMemberMap(
                    round.getId());

            // 라운드 내 모든 활성 멤버 ID 수집 (닉네임 일괄 조회용)
            Set<Long> allActiveMemberIds = groupMemberMap.values().stream()
                    .flatMap(List::stream)
                    .filter(participantIds::contains)
                    .filter(id -> !excludedMemberIds.contains(id))
                    .collect(Collectors.toSet());
            Map<Long, String> nicknameMap = this.orgMemberService
                    .getNicknameMap(new ArrayList<>(allActiveMemberIds));

            for (Order order : orders) {
                List<Long> eligibleIds = groupMemberMap
                        .getOrDefault(order.getGroupId(), List.of()).stream()
                        .filter(participantIds::contains)
                        .filter(id -> !excludedMemberIds.contains(id))
                        .toList();

                if (eligibleIds.isEmpty()) {
                    continue;
                }

                int memberCount = eligibleIds.size();
                BigDecimal perPerson = MoneyCalculator.divide(
                        order.getTotalPrice(), memberCount).quotient();

                List<OrderMemberShareResponse> members = eligibleIds.stream()
                        .map(memberId -> OrderMemberShareResponse.builder()
                                .orgMemberId(memberId)
                                .nickname(nicknameMap.getOrDefault(memberId, ""))
                                .shareRatio(1)
                                .totalRatio(memberCount)
                                .amount(perPerson)
                                .build())
                        .toList();

                OrderSettlementItemResponse item = OrderSettlementItemResponse.builder()
                        .roundId(round.getId())
                        .menuId(order.getMenuId())
                        .menuName(order.getMenuName())
                        .menuPrice(order.getMenuPrice())
                        .quantity(order.getQuantity())
                        .totalPrice(order.getTotalPrice())
                        .members(members)
                        .build();

                String category = order.getMenuCategory();

                categoryItemsMap
                        .computeIfAbsent(category, k -> new ArrayList<>())
                        .add(item);

                totalAmount = totalAmount.add(order.getTotalPrice());
            }
        }

        List<OrderGroupResponse> orderGroups = this.buildOrderGroups(categoryItemsMap);

        return OrderSettlementResponse.builder()
                .sessionId(sessionId)
                .totalAmount(totalAmount)
                .strategy(strategyType)
                .orderGroups(orderGroups)
                .build();
    }

    /**
     * 특정 그룹의 주문 내역을 카테고리 > 메뉴별로 그룹핑하여 조회한다.
     *
     * @param roundId 라운드 ID
     * @param groupId 그룹 ID
     * @return 그룹별 주문 내역 응답
     */
    public NodeOrdersResponse getGroupOrders(Long roundId, Long groupId) {
        this.findRoundById(roundId);

        // 자기 자신 + 상위 그룹 ID 수집 (하위 그룹 조회 시 상위 주문 포함)
        List<Long> ancestorGroupIds = this.collectAncestorGroupIds(roundId, groupId);

        // 단일 라운드에서 해당 그룹 및 조상 그룹 주문 수집
        List<Order> groupOrders = this.orderRepository
                .findByRoundIdAndGroupIdIn(roundId, ancestorGroupIds);

        // 그룹 이름: 스냅샷 우선, 없으면 RoundGroupRepository 조회
        String groupName = groupOrders.stream()
                .map(Order::getGroupName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseGet(() -> this.roundGroupRepository.findById(groupId)
                        .map(RoundGroup::getName)
                        .orElse(""));

        if (groupOrders.isEmpty()) {
            return NodeOrdersResponse.builder()
                    .groupId(groupId)
                    .groupName(groupName)
                    .totalAmount(BigDecimal.ZERO)
                    .categories(List.of())
                    .build();
        }

        // OrderDetail 일괄 조회 (N+1 방지)
        List<Long> orderIds = groupOrders.stream().map(Order::getId).toList();
        List<OrderDetail> allDetails = this.orderDetailRepository.findByOrderIdIn(orderIds);
        Map<Long, List<OrderDetail>> detailsByOrderId = allDetails.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        // 닉네임 일괄 조회
        Set<Long> allMemberIds = allDetails.stream()
                .map(OrderDetail::getOrgMemberId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = this.orgMemberService
                .getNicknameMap(new ArrayList<>(allMemberIds));

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 카테고리 → (menuId → 주문 목록) 그룹핑
        Map<String, Map<Long, List<Order>>> categoryMenuOrdersMap = new LinkedHashMap<>();
        for (Order order : groupOrders) {
            String category = order.getMenuCategory();
            categoryMenuOrdersMap
                    .computeIfAbsent(category, k -> new LinkedHashMap<>())
                    .computeIfAbsent(order.getMenuId(), k -> new ArrayList<>())
                    .add(order);
            totalAmount = totalAmount.add(order.getTotalPrice());
        }

        // 카테고리 → NodeCategoryGroupResponse 변환
        List<NodeCategoryGroupResponse> categories = new ArrayList<>();

        // null 카테고리 제외한 것 먼저 알파벳 정렬
        List<String> sortedCategories = categoryMenuOrdersMap.keySet().stream()
                .filter(c -> c != null)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        if (categoryMenuOrdersMap.containsKey(null)) {
            sortedCategories.add(null);
        }

        for (String category : sortedCategories) {
            Map<Long, List<Order>> menuOrdersMap = categoryMenuOrdersMap.get(category);
            List<NodeMenuSummaryResponse> menus = new ArrayList<>();
            BigDecimal categoryTotal = BigDecimal.ZERO;

            for (Map.Entry<Long, List<Order>> menuEntry : menuOrdersMap.entrySet()) {
                Long menuId = menuEntry.getKey();
                List<Order> menuOrders = menuEntry.getValue();

                BigDecimal menuTotalPrice = menuOrders.stream()
                        .map(Order::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                categoryTotal = categoryTotal.add(menuTotalPrice);

                // 참여자별 주문 횟수 집계 (Map<orgMemberId, count>)
                Map<Long, Integer> memberCountMap = new LinkedHashMap<>();
                List<NodeOrderHistoryResponse> orderHistories = new ArrayList<>();

                for (Order order : menuOrders) {
                    List<OrderDetail> details = detailsByOrderId
                            .getOrDefault(order.getId(), List.of());

                    List<String> participantNicknames = details.stream()
                            .map(d -> nicknameMap.getOrDefault(d.getOrgMemberId(), ""))
                            .toList();

                    for (OrderDetail detail : details) {
                        memberCountMap.merge(detail.getOrgMemberId(), 1, Integer::sum);
                    }

                    orderHistories.add(NodeOrderHistoryResponse.builder()
                            .orderId(order.getId())
                            .menuPrice(order.getMenuPrice())
                            .quantity(order.getQuantity())
                            .totalPrice(order.getTotalPrice())
                            .memberNicknames(participantNicknames)
                            .createdDate(order.getCreatedDate())
                            .build());
                }

                List<NodeMemberCountResponse> memberCounts = memberCountMap.entrySet().stream()
                        .map(e -> NodeMemberCountResponse.builder()
                                .orgMemberId(e.getKey())
                                .nickname(nicknameMap.getOrDefault(e.getKey(), ""))
                                .count(e.getValue())
                                .build())
                        .toList();

                // menuName 스냅샷: 해당 menuId 첫 번째 주문에서 추출
                String menuName = menuOrders.get(0).getMenuName();

                menus.add(NodeMenuSummaryResponse.builder()
                        .menuId(menuId)
                        .menuName(menuName)
                        .totalPrice(menuTotalPrice)
                        .orderCount(menuOrders.size())
                        .members(memberCounts)
                        .orders(orderHistories)
                        .build());
            }

            categories.add(NodeCategoryGroupResponse.builder()
                    .category(category)
                    .totalAmount(categoryTotal)
                    .menus(menus)
                    .build());
        }

        return NodeOrdersResponse.builder()
                .groupId(groupId)
                .groupName(groupName)
                .totalAmount(totalAmount)
                .categories(categories)
                .build();
    }

    /**
     * 특정 그룹의 주문만으로 멤버별 정산 금액을 계산한다.
     *
     * <p>하위 그룹 조회 시 상위 그룹의 주문도 포함된다.</p>
     *
     * @param roundId      라운드 ID
     * @param groupId      그룹 ID
     * @param strategyType 나머지 분배 전략
     * @return 그룹별 정산 응답
     */
    public NodeSettlementResponse calculateGroupSettlement(
            Long roundId, Long groupId, RemainderStrategyType strategyType) {
        Round round = this.findRoundById(roundId);
        Session session = this.findSessionById(round.getSessionId());

        // 자기 자신 + 상위 그룹 ID 수집 (하위 그룹 조회 시 상위 주문 포함)
        List<Long> ancestorGroupIds = this.collectAncestorGroupIds(roundId, groupId);

        // 단일 라운드에서 해당 그룹 및 조상 그룹 주문 필터
        List<Order> groupOrders = this.orderRepository
                .findByRoundIdAndGroupIdIn(roundId, ancestorGroupIds);

        // 그룹 이름 스냅샷 추출
        String groupName = groupOrders.stream()
                .map(Order::getGroupName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElseGet(() -> this.roundGroupRepository.findById(groupId)
                        .map(RoundGroup::getName)
                        .orElse(""));

        Map<Long, BigDecimal> totalByMember = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (!groupOrders.isEmpty()) {
            List<RoundParticipant> participants = this.roundParticipantRepository
                    .findByRoundId(roundId);

            Map<Long, List<Long>> groupMemberMap = this.buildGroupDescendantMemberMap(roundId);
            Map<Long, BigDecimal> roundAmounts = this.calculateMemberAmounts(
                    groupOrders, participants, groupMemberMap);

            BigDecimal roundTotal = groupOrders.stream()
                    .map(Order::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.applyRemainderStrategy(roundAmounts, roundTotal, participants,
                    session.getOwnerId(), strategyType);

            totalByMember = roundAmounts;
            totalAmount = roundTotal;
        }

        List<Long> memberIds = totalByMember.keySet().stream().toList();
        Map<Long, String> nicknameMap = this.orgMemberService.getNicknameMap(memberIds);

        List<MemberAmountResponse> settlements = totalByMember.entrySet().stream()
                .map(entry -> MemberAmountResponse.builder()
                        .orgMemberId(entry.getKey())
                        .nickname(nicknameMap.getOrDefault(entry.getKey(), ""))
                        .amount(entry.getValue())
                        .isExcluded(false)
                        .isPaid(false)
                        .paidAmount(BigDecimal.ZERO)
                        .remainingAmount(entry.getValue())
                        .build())
                .toList();

        return NodeSettlementResponse.builder()
                .groupId(groupId)
                .groupName(groupName)
                .totalAmount(totalAmount)
                .strategy(strategyType)
                .settlements(settlements)
                .build();
    }

    /**
     * 배치 조회된 데이터를 이용하여 단일 라운드의 정산을 계산한다.
     *
     * <p>{@code calculateSessionSettlement}에서 배치 페치한 데이터를 라운드 단위로
     * 메모리 그룹핑 후 전달받아 처리한다. DB 쿼리가 발생하지 않는다.</p>
     *
     * @param round                round 엔티티
     * @param ownerId              세션 소유자 userId
     * @param strategyType         나머지 분배 전략
     * @param participants         해당 라운드의 참여자 목록
     * @param orders               해당 라운드의 주문 목록
     * @param groups               해당 라운드의 그룹 목록
     * @param groupMembersByGroupId 전체 그룹 멤버 맵 (groupId → 멤버 목록)
     * @param nicknameMap          전체 닉네임 맵 (orgMemberId → nickname)
     * @param orgMemberMap         전체 OrgMember 맵 (orgMemberId → OrgMember)
     * @return 라운드 정산 응답
     */
    private RoundSettlementResponse calculateRoundSettlementFromBatch(
            Round round, Long ownerId, RemainderStrategyType strategyType,
            List<RoundParticipant> participants, List<Order> orders,
            List<RoundGroup> groups,
            Map<Long, List<RoundGroupMember>> groupMembersByGroupId,
            Map<Long, String> nicknameMap,
            Map<Long, OrgMember> orgMemberMap) {

        Map<Long, List<Long>> groupMemberMap = this.buildGroupDescendantMemberMapFromData(
                groups, groupMembersByGroupId);

        Map<Long, BigDecimal> memberAmounts = this.calculateMemberAmounts(
                orders, participants, groupMemberMap);

        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.applyRemainderStrategyFromBatch(memberAmounts, totalAmount, participants,
                ownerId, strategyType, orgMemberMap);

        Set<Long> excludedIds = participants.stream()
                .filter(RoundParticipant::isExcluded)
                .map(RoundParticipant::getOrgMemberId)
                .collect(Collectors.toSet());

        List<MemberAmountResponse> settlements = participants.stream()
                .map(p -> MemberAmountResponse.builder()
                        .orgMemberId(p.getOrgMemberId())
                        .nickname(nicknameMap.getOrDefault(p.getOrgMemberId(), ""))
                        .amount(memberAmounts.getOrDefault(p.getOrgMemberId(), BigDecimal.ZERO))
                        .isExcluded(excludedIds.contains(p.getOrgMemberId()))
                        .isPaid(false)
                        .paidAmount(BigDecimal.ZERO)
                        .remainingAmount(memberAmounts.getOrDefault(p.getOrgMemberId(), BigDecimal.ZERO))
                        .build())
                .toList();

        return RoundSettlementResponse.builder()
                .roundId(round.getId())
                .totalAmount(totalAmount)
                .strategy(strategyType)
                .settlements(settlements)
                .build();
    }

    /**
     * 배치 조회된 그룹·멤버 데이터로 그룹별 하위 멤버 맵을 빌드한다.
     *
     * <p>DB 쿼리 없이 전달받은 인메모리 데이터만 사용한다.</p>
     *
     * @param groups               라운드 그룹 목록
     * @param groupMembersByGroupId 그룹 ID → 멤버 목록 맵
     * @return 그룹 ID → 해당 그룹 + 모든 하위 그룹 멤버 ID 목록
     */
    private Map<Long, List<Long>> buildGroupDescendantMemberMapFromData(
            List<RoundGroup> groups,
            Map<Long, List<RoundGroupMember>> groupMembersByGroupId) {

        Map<Long, List<RoundGroup>> childrenMap = groups.stream()
                .filter(g -> g.getParentGroupId() != null)
                .collect(Collectors.groupingBy(RoundGroup::getParentGroupId));

        Map<Long, List<Long>> membersByGroupId = groups.stream()
                .collect(Collectors.toMap(
                        RoundGroup::getId,
                        g -> groupMembersByGroupId.getOrDefault(g.getId(), List.of()).stream()
                                .map(RoundGroupMember::getOrgMemberId)
                                .toList()));

        Map<Long, List<Long>> result = new HashMap<>();
        for (RoundGroup group : groups) {
            List<Long> descendantGroupIds = new ArrayList<>();
            this.collectDescendantGroupIds(group.getId(), descendantGroupIds, childrenMap);
            List<Long> members = descendantGroupIds.stream()
                    .flatMap(gid -> membersByGroupId.getOrDefault(gid, List.of()).stream())
                    .distinct().toList();
            result.put(group.getId(), members);
        }
        return result;
    }

    /**
     * 배치 조회된 OrgMember 맵을 이용하여 나머지 분배 전략을 적용한다.
     *
     * <p>DB 쿼리 없이 전달받은 인메모리 맵을 활용하여 owner 인덱스를 결정한다.</p>
     *
     * @param memberAmounts  멤버별 정산 금액 맵
     * @param totalAmount    총 정산 금액
     * @param participants   라운드 참여자 목록
     * @param ownerId        세션 소유자 userId
     * @param strategyType   나머지 분배 전략
     * @param orgMemberMap   orgMemberId → OrgMember 맵
     */
    private void applyRemainderStrategyFromBatch(Map<Long, BigDecimal> memberAmounts,
                                                  BigDecimal totalAmount,
                                                  List<RoundParticipant> participants,
                                                  Long ownerId,
                                                  RemainderStrategyType strategyType,
                                                  Map<Long, OrgMember> orgMemberMap) {
        List<Long> activeMemberIds = participants.stream()
                .filter(p -> !p.isExcluded())
                .map(RoundParticipant::getOrgMemberId)
                .toList();

        if (activeMemberIds.isEmpty()) {
            return;
        }

        BigDecimal currentSum = activeMemberIds.stream()
                .map(id -> memberAmounts.getOrDefault(id, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainder = totalAmount.subtract(currentSum);

        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Map<Integer, BigDecimal> indexedAmounts = new HashMap<>();
        int ownerIndex = 0;

        for (int i = 0; i < activeMemberIds.size(); i++) {
            Long memberId = activeMemberIds.get(i);
            indexedAmounts.put(i, memberAmounts.getOrDefault(memberId, BigDecimal.ZERO));

            OrgMember orgMember = orgMemberMap.get(memberId);
            if (orgMember != null && ownerId.equals(orgMember.getUserId())) {
                ownerIndex = i;
            }
        }

        RemainderStrategy strategy = this.resolveStrategy(strategyType);
        strategy.apply(indexedAmounts, remainder, ownerIndex);

        for (int i = 0; i < activeMemberIds.size(); i++) {
            memberAmounts.put(activeMemberIds.get(i), indexedAmounts.get(i));
        }
    }

    private List<OrderGroupResponse> buildOrderGroups(
            Map<String, List<OrderSettlementItemResponse>> categoryItemsMap) {

        List<OrderGroupResponse> groups = new ArrayList<>();
        List<OrderSettlementItemResponse> nullCategoryItems = null;

        // 알파벳순 정렬 (null 제외)
        List<String> sortedCategories = categoryItemsMap.keySet().stream()
                .filter(c -> c != null)
                .sorted(Comparator.naturalOrder())
                .toList();

        for (String category : sortedCategories) {
            List<OrderSettlementItemResponse> items = categoryItemsMap.get(category);
            BigDecimal groupTotal = items.stream()
                    .map(OrderSettlementItemResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(OrderGroupResponse.builder()
                    .category(category)
                    .totalAmount(groupTotal)
                    .items(items)
                    .build());
        }

        // null 카테고리는 마지막에 추가
        if (categoryItemsMap.containsKey(null)) {
            nullCategoryItems = categoryItemsMap.get(null);
            BigDecimal groupTotal = nullCategoryItems.stream()
                    .map(OrderSettlementItemResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(OrderGroupResponse.builder()
                    .category(null)
                    .totalAmount(groupTotal)
                    .items(nullCategoryItems)
                    .build());
        }

        return groups;
    }

    /**
     * 그룹 계층 기반으로 각 주문의 분배 대상 멤버를 결정하고 균등 분배한다.
     *
     * @param orders                   주문 목록
     * @param participants             라운드 참여자 목록
     * @param groupDescendantMemberMap 그룹별 (자신 + 하위) 전체 멤버 ID 맵
     * @return 멤버별 정산 금액
     */
    private Map<Long, BigDecimal> calculateMemberAmounts(
            List<Order> orders, List<RoundParticipant> participants,
            Map<Long, List<Long>> groupDescendantMemberMap) {

        Map<Long, BigDecimal> memberAmounts = new HashMap<>();
        Set<Long> excludedMemberIds = participants.stream()
                .filter(RoundParticipant::isExcluded)
                .map(RoundParticipant::getOrgMemberId)
                .collect(Collectors.toSet());

        participants.forEach(p -> memberAmounts.put(p.getOrgMemberId(), BigDecimal.ZERO));

        if (orders.isEmpty()) {
            return memberAmounts;
        }

        Set<Long> participantIds = memberAmounts.keySet();

        for (Order order : orders) {
            List<Long> eligibleMemberIds = groupDescendantMemberMap
                    .getOrDefault(order.getGroupId(), List.of()).stream()
                    .filter(participantIds::contains)
                    .filter(id -> !excludedMemberIds.contains(id))
                    .toList();

            if (eligibleMemberIds.isEmpty()) {
                continue;
            }

            MoneyCalculator.DivisionResult division = MoneyCalculator.divide(
                    order.getTotalPrice(), eligibleMemberIds.size());
            for (int i = 0; i < eligibleMemberIds.size(); i++) {
                BigDecimal share = division.quotient();
                if (i == eligibleMemberIds.size() - 1) {
                    share = share.add(division.remainder());
                }
                memberAmounts.merge(eligibleMemberIds.get(i), share, BigDecimal::add);
            }
        }

        return memberAmounts;
    }

    /**
     * 라운드 내 각 그룹별로 (자신 + 하위 그룹)의 전체 멤버 ID 맵을 빌드한다.
     *
     * @param roundId 라운드 ID
     * @return 그룹 ID → 해당 그룹 + 모든 하위 그룹 멤버 ID 목록
     */
    private Map<Long, List<Long>> buildGroupDescendantMemberMap(Long roundId) {
        List<RoundGroup> allGroups = this.roundGroupRepository.findByRoundId(roundId);
        Map<Long, List<RoundGroup>> childrenMap = allGroups.stream()
                .filter(g -> g.getParentGroupId() != null)
                .collect(Collectors.groupingBy(RoundGroup::getParentGroupId));

        List<Long> allGroupIds = allGroups.stream().map(RoundGroup::getId).toList();
        List<RoundGroupMember> allMembers = this.roundGroupMemberRepository
                .findByGroupIdIn(allGroupIds);
        Map<Long, List<Long>> membersByGroupId = allMembers.stream()
                .collect(Collectors.groupingBy(
                        RoundGroupMember::getGroupId,
                        Collectors.mapping(RoundGroupMember::getOrgMemberId, Collectors.toList())));

        Map<Long, List<Long>> result = new HashMap<>();
        for (RoundGroup group : allGroups) {
            List<Long> descendantGroupIds = new ArrayList<>();
            this.collectDescendantGroupIds(group.getId(), descendantGroupIds, childrenMap);
            List<Long> members = descendantGroupIds.stream()
                    .flatMap(gid -> membersByGroupId.getOrDefault(gid, List.of()).stream())
                    .distinct().toList();
            result.put(group.getId(), members);
        }
        return result;
    }

    private void collectDescendantGroupIds(Long groupId, List<Long> collected,
                                            Map<Long, List<RoundGroup>> childrenMap) {
        collected.add(groupId);
        childrenMap.getOrDefault(groupId, List.of())
                .forEach(child -> this.collectDescendantGroupIds(
                        child.getId(), collected, childrenMap));
    }

    private void applyRemainderStrategy(Map<Long, BigDecimal> memberAmounts,
                                         BigDecimal totalAmount,
                                         List<RoundParticipant> participants,
                                         Long ownerId,
                                         RemainderStrategyType strategyType) {
        List<Long> activeMemberIds = participants.stream()
                .filter(p -> !p.isExcluded())
                .map(RoundParticipant::getOrgMemberId)
                .toList();

        if (activeMemberIds.isEmpty()) {
            return;
        }

        BigDecimal currentSum = activeMemberIds.stream()
                .map(id -> memberAmounts.getOrDefault(id, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainder = totalAmount.subtract(currentSum);

        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Map<Integer, BigDecimal> indexedAmounts = new HashMap<>();
        int ownerIndex = 0;

        Map<Long, OrgMember> orgMemberMap = this.orgMemberRepository
                .findAllById(activeMemberIds).stream()
                .collect(Collectors.toMap(OrgMember::getId, Function.identity()));

        for (int i = 0; i < activeMemberIds.size(); i++) {
            Long memberId = activeMemberIds.get(i);
            indexedAmounts.put(i, memberAmounts.getOrDefault(memberId, BigDecimal.ZERO));

            OrgMember orgMember = orgMemberMap.get(memberId);
            if (orgMember != null && ownerId.equals(orgMember.getUserId())) {
                ownerIndex = i;
            }
        }

        RemainderStrategy strategy = this.resolveStrategy(strategyType);
        strategy.apply(indexedAmounts, remainder, ownerIndex);

        for (int i = 0; i < activeMemberIds.size(); i++) {
            memberAmounts.put(activeMemberIds.get(i), indexedAmounts.get(i));
        }
    }

    private RemainderStrategy resolveStrategy(RemainderStrategyType type) {
        return switch (type) {
            case OWNER -> new OwnerRemainderStrategy();
            case RANDOM -> new RandomRemainderStrategy();
            case ROUND_UP -> new RoundUpRemainderStrategy();
        };
    }

    /**
     * 지정된 groupId부터 루트까지의 모든 조상 groupId를 수집한다 (자기 자신 포함).
     *
     * <p>라운드 내 전체 그룹을 한 번에 조회하여 인메모리 parent map으로 탐색한다.</p>
     *
     * @param roundId 라운드 ID
     * @param groupId 시작 그룹 ID
     * @return 자신 포함 조상 groupId 목록
     */
    private List<Long> collectAncestorGroupIds(Long roundId, Long groupId) {
        List<RoundGroup> allGroups = this.roundGroupRepository.findByRoundId(roundId);
        Map<Long, Long> parentMap = allGroups.stream()
                .collect(Collectors.toMap(RoundGroup::getId, g ->
                        g.getParentGroupId() != null ? g.getParentGroupId() : -1L));

        List<Long> groupIds = new ArrayList<>();
        Long current = groupId;
        while (current != null && parentMap.containsKey(current)) {
            groupIds.add(current);
            Long parentId = parentMap.get(current);
            current = parentId.equals(-1L) ? null : parentId;
        }
        return groupIds;
    }

    private Round findRoundById(Long roundId) {
        return this.roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_NOT_FOUND));
    }

    private Session findSessionById(Long sessionId) {
        return this.sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SESSION_NOT_FOUND));
    }

}
