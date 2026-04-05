package com.dirtypay.domain.group.service;

import com.dirtypay.domain.group.dto.request.GroupCreateRequest;
import com.dirtypay.domain.group.dto.request.GroupUpdateRequest;
import com.dirtypay.domain.group.dto.request.SharedMenuSaveRequest;
import com.dirtypay.domain.group.dto.response.GroupMemberResponse;
import com.dirtypay.domain.group.dto.response.GroupResponse;
import com.dirtypay.domain.group.dto.response.PersonalOrderResponse;
import com.dirtypay.domain.group.dto.response.SharedMenuResponse;
import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.entity.RoundGroupSharedMenu;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.group.repository.RoundGroupSharedMenuRepository;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
import com.dirtypay.domain.round.repository.RoundParticipantRepository;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 그룹 서비스.
 *
 * <p>그룹 CRUD, 참여/탈퇴, 공유 메뉴 관리 비즈니스 로직을 처리한다.
 * N+1 문제를 방지하기 위해 수동 배치 fetch를 사용한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupMemberRepository roundGroupMemberRepository;
    private final RoundGroupSharedMenuRepository roundGroupSharedMenuRepository;
    private final RoundRepository roundRepository;
    private final RoundParticipantRepository roundParticipantRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MemberRepository memberRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 라운드의 그룹 목록을 트리 구조로 조회한다.
     *
     * <p>배치 페치로 N+1 문제를 방지하고, 인메모리에서 트리를 빌드한다.
     * 각 그룹의 totalAmount를 재귀적으로 계산한다.</p>
     *
     * @param roundId 라운드 ID
     * @param userId  현재 사용자 ID
     * @return 그룹 트리 목록 (루트 그룹 리스트)
     */
    public List<GroupResponse> getGroups(Long roundId, Long userId) {
        Round round = this.findRoundById(roundId);

        // 현재 사용자의 OrgMember 조회
        OrgMember currentOrgMember = this.orgMemberRepository
                .findBySessionIdAndUserId(round.getSessionId(), userId)
                .orElse(null);
        Long currentOrgMemberId = currentOrgMember != null ? currentOrgMember.getId() : null;

        // 1. 배치 페치: 그룹, 멤버, 공유 메뉴
        List<RoundGroup> groups = this.roundGroupRepository.findByRoundId(roundId);
        if (groups.isEmpty()) {
            return List.of();
        }

        List<Long> groupIds = groups.stream().map(RoundGroup::getId).toList();

        List<RoundGroupMember> allGroupMembers = this.roundGroupMemberRepository.findByGroupIdIn(groupIds);
        List<RoundGroupSharedMenu> allSharedMenus = this.roundGroupSharedMenuRepository.findByGroupIdIn(groupIds);

        // 2. 메뉴 배치 조회 (라운드에 연결된 가게의 StoreMenu 전체 조회)
        List<StoreMenu> storeMenus = this.storeMenuRepository.findAllByStoreId(round.getStoreId());
        Map<Long, StoreMenu> menuMap = storeMenus.stream()
                .collect(Collectors.toMap(StoreMenu::getId, Function.identity()));

        // 3. OrgMember 배치 조회
        List<Long> orgMemberIds = allGroupMembers.stream()
                .map(RoundGroupMember::getOrgMemberId).distinct().toList();
        Map<Long, OrgMember> orgMemberMap = this.orgMemberRepository.findAllById(orgMemberIds).stream()
                .collect(Collectors.toMap(OrgMember::getId, Function.identity()));

        // 4. 주문/주문상세 배치 조회 (개인 주문용)
        List<Order> orders = this.orderRepository.findByRoundId(roundId);
        Map<Long, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderDetail> allOrderDetails = List.of();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            allOrderDetails = this.orderDetailRepository.findByOrderIdIn(orderIds);
        }

        // orgMemberId별 OrderDetail 그룹핑
        Map<Long, List<OrderDetail>> detailsByOrgMemberId = allOrderDetails.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrgMemberId));

        // 5. Map 빌드
        Map<Long, List<RoundGroupMember>> membersByGroupId = allGroupMembers.stream()
                .collect(Collectors.groupingBy(RoundGroupMember::getGroupId));
        Map<Long, List<RoundGroupSharedMenu>> sharedMenusByGroupId = allSharedMenus.stream()
                .collect(Collectors.groupingBy(RoundGroupSharedMenu::getGroupId));

        // 그룹에 참여한 orgMemberId 세트 (isParticipating 판단용)
        Set<Long> currentUserGroupIds = allGroupMembers.stream()
                .filter(gm -> gm.getOrgMemberId().equals(currentOrgMemberId))
                .map(RoundGroupMember::getGroupId)
                .collect(Collectors.toSet());

        // 6. GroupResponse 스켈레톤 생성
        Map<Long, GroupResponse> responseMap = new LinkedHashMap<>();
        for (RoundGroup group : groups) {
            List<SharedMenuResponse> sharedMenuResponses = sharedMenusByGroupId
                    .getOrDefault(group.getId(), List.of()).stream()
                    .map(sm -> {
                        StoreMenu menu = menuMap.get(sm.getMenuId());
                        return SharedMenuResponse.builder()
                                .menuId(sm.getMenuId())
                                .menuName(menu != null ? menu.getName() : "")
                                .price(menu != null ? menu.getPrice() : BigDecimal.ZERO)
                                .quantity(sm.getQuantity())
                                .build();
                    })
                    .toList();

            List<GroupMemberResponse> memberResponses = membersByGroupId
                    .getOrDefault(group.getId(), List.of()).stream()
                    .map(gm -> {
                        OrgMember orgMember = orgMemberMap.get(gm.getOrgMemberId());
                        List<PersonalOrderResponse> personalOrders = this.buildPersonalOrders(
                                gm.getOrgMemberId(), detailsByOrgMemberId, orderMap);
                        BigDecimal memberTotal = personalOrders.stream()
                                .map(PersonalOrderResponse::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return GroupMemberResponse.builder()
                                .orgMemberId(gm.getOrgMemberId())
                                .nickname(orgMember != null ? orgMember.getNickname() : "")
                                .isCurrentUser(gm.getOrgMemberId().equals(currentOrgMemberId))
                                .personalOrders(personalOrders)
                                .totalAmount(memberTotal)
                                .build();
                    })
                    .toList();

            GroupResponse response = GroupResponse.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .parentGroupId(group.getParentGroupId())
                    .depth(group.getDepth())
                    .isParticipating(currentUserGroupIds.contains(group.getId()))
                    .sharedMenus(sharedMenuResponses)
                    .members(memberResponses)
                    .childGroups(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .build();

            responseMap.put(group.getId(), response);
        }

        // 7. flat → tree 빌드
        List<GroupResponse> roots = new ArrayList<>();
        for (GroupResponse response : responseMap.values()) {
            if (response.getParentGroupId() == null) {
                roots.add(response);
            } else {
                GroupResponse parent = responseMap.get(response.getParentGroupId());
                if (parent != null) {
                    parent.getChildGroups().add(response);
                } else {
                    roots.add(response);
                }
            }
        }

        // 8. totalAmount 재귀 계산 (bottom-up)
        for (GroupResponse root : roots) {
            this.calculateTotalAmount(root);
        }

        return roots;
    }

    /**
     * 새로운 그룹을 생성한다.
     *
     * <p>생성자는 자동 참여되지 않으므로 별도로 {@code joinGroup}을 호출해야 한다.
     * parentGroupId가 지정되면 같은 라운드 소속인지 검증한다.</p>
     *
     * @param roundId 라운드 ID
     * @param request 그룹 생성 요청
     * @param userId  현재 사용자 ID
     * @return 생성된 그룹 응답
     */
    @Transactional
    public GroupResponse createGroup(Long roundId, GroupCreateRequest request, Long userId) {
        Round round = this.findRoundById(roundId);
        round.verifyOpen();

        int depth = 0;
        if (request.getParentGroupId() != null) {
            RoundGroup parentGroup = this.findGroupById(request.getParentGroupId());
            if (!parentGroup.getRoundId().equals(roundId)) {
                throw new BusinessException(ErrorCode.GROUP_ROUND_MISMATCH);
            }
            depth = parentGroup.getDepth() + 1;
        }

        RoundGroup group = RoundGroup.builder()
                .roundId(roundId)
                .parentGroupId(request.getParentGroupId())
                .name(request.getName())
                .depth(depth)
                .build();
        RoundGroup savedGroup = this.roundGroupRepository.save(group);

        return GroupResponse.builder()
                .groupId(savedGroup.getId())
                .groupName(savedGroup.getName())
                .parentGroupId(savedGroup.getParentGroupId())
                .depth(savedGroup.getDepth())
                .isParticipating(false)
                .sharedMenus(List.of())
                .members(List.of())
                .childGroups(List.of())
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    /**
     * 그룹명을 수정한다.
     *
     * @param groupId 그룹 ID
     * @param request 수정 요청
     * @param userId  현재 사용자 ID
     * @return 수정된 그룹 응답
     */
    @Transactional
    public GroupResponse updateGroup(Long groupId, GroupUpdateRequest request, Long userId) {
        RoundGroup group = this.findGroupById(groupId);
        this.findRoundById(group.getRoundId()).verifyOpen();
        group.updateName(request.getName());

        return this.buildSingleGroupResponse(group, userId);
    }

    /**
     * 그룹을 삭제한다. (Soft Delete)
     *
     * <p>참여 중인 멤버가 있으면 삭제할 수 없다.
     * 멤버가 없을 때만 하위 그룹 및 관련 데이터(공유 메뉴)를 함께 삭제한다.</p>
     *
     * @param groupId 그룹 ID
     * @throws BusinessException 참여 중인 멤버가 있는 경우
     */
    @Transactional
    public void deleteGroup(Long groupId) {
        RoundGroup group = this.findGroupById(groupId);
        this.findRoundById(group.getRoundId()).verifyOpen();

        List<RoundGroup> allRoundGroups = this.roundGroupRepository.findByRoundId(group.getRoundId());
        Map<Long, List<RoundGroup>> childrenMap = allRoundGroups.stream()
                .filter(g -> g.getParentGroupId() != null)
                .collect(Collectors.groupingBy(RoundGroup::getParentGroupId));

        List<RoundGroup> allGroups = new ArrayList<>();
        this.collectDescendantsInMemory(group, allGroups, childrenMap);

        List<Long> allGroupIds = allGroups.stream().map(RoundGroup::getId).toList();

        // 참여 중인 멤버가 있으면 삭제 불가
        List<RoundGroupMember> members = this.roundGroupMemberRepository.findByGroupIdIn(allGroupIds);
        if (!members.isEmpty()) {
            throw new BusinessException(ErrorCode.GROUP_HAS_MEMBERS);
        }

        // 공유 메뉴 soft delete
        List<RoundGroupSharedMenu> sharedMenus = this.roundGroupSharedMenuRepository.findByGroupIdIn(allGroupIds);
        sharedMenus.forEach(RoundGroupSharedMenu::delete);

        // 그룹 soft delete
        allGroups.forEach(RoundGroup::delete);
    }

    /**
     * 현재 사용자가 그룹에 참여한다.
     *
     * <p>애플리케이션 레벨 중복 검사 이후에도, 동시 요청으로 인한 Race Condition을
     * DB UNIQUE 제약(uk_group_member)이 최종 방어한다.
     * {@link DataIntegrityViolationException} 발생 시 {@link ErrorCode#GROUP_ALREADY_JOINED}
     * 비즈니스 예외로 변환하여 일관된 응답을 반환한다.</p>
     *
     * @param groupId 그룹 ID
     * @param userId  현재 사용자 ID
     * @throws BusinessException 이미 그룹에 참여 중이거나, 라운드 내 다른 그룹에 참여 중인 경우
     */
    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        RoundGroup group = this.findGroupById(groupId);
        Round round = this.findRoundById(group.getRoundId());
        round.verifyOpen();

        OrgMember orgMember = this.resolveOrgMember(round.getSessionId(), userId);

        if (this.roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, orgMember.getId())) {
            throw new BusinessException(ErrorCode.GROUP_ALREADY_JOINED);
        }

        // 라운드 내 다른 그룹 참여 여부 확인 (1인 1그룹 제약)
        if (this.roundGroupMemberRepository.existsByRoundIdAndOrgMemberId(group.getRoundId(), orgMember.getId())) {
            throw new BusinessException(ErrorCode.GROUP_ALREADY_IN_ROUND);
        }

        try {
            RoundGroupMember groupMember = RoundGroupMember.builder()
                    .groupId(groupId)
                    .orgMemberId(orgMember.getId())
                    .build();
            this.roundGroupMemberRepository.saveAndFlush(groupMember);
        } catch (DataIntegrityViolationException e) {
            // DB UNIQUE 제약(uk_group_member) 위반: 동시 요청으로 인한 Race Condition 방어
            throw new BusinessException(ErrorCode.GROUP_ALREADY_JOINED);
        }

        // RoundParticipant 동기화: resolveOrgMember가 기존 멤버를 반환할 수 있으므로 중복 확인
        if (!this.roundParticipantRepository.existsByRoundIdAndOrgMemberId(round.getId(), orgMember.getId())) {
            this.roundParticipantRepository.save(
                    RoundParticipant.builder().roundId(round.getId()).orgMemberId(orgMember.getId()).build());
        }
    }

    /**
     * 현재 사용자가 그룹에서 탈퇴한다.
     *
     * @param groupId 그룹 ID
     * @param userId  현재 사용자 ID
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        RoundGroup group = this.findGroupById(groupId);
        Round round = this.findRoundById(group.getRoundId());
        round.verifyOpen();

        OrgMember orgMember = this.resolveOrgMember(round.getSessionId(), userId);

        RoundGroupMember groupMember = this.roundGroupMemberRepository
                .findByGroupIdAndOrgMemberId(groupId, orgMember.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_JOINED));

        this.roundGroupMemberRepository.delete(groupMember);
    }

    /**
     * 사용자의 그룹을 변경한다. (from 그룹 탈퇴 + to 그룹 참여를 단일 트랜잭션으로 처리)
     *
     * <p>두 그룹이 같은 라운드에 속하는지 검증하고,
     * 기존 멤버십을 soft delete 한 뒤 새 멤버십을 생성한다.</p>
     *
     * @param fromGroupId 현재 참여 중인 그룹 ID
     * @param toGroupId   이동할 대상 그룹 ID
     * @param userId      현재 사용자 ID
     * @throws BusinessException 같은 그룹이거나, 같은 라운드가 아니거나, 참여/미참여 조건 위반 시
     */
    @Transactional
    public void changeGroup(Long fromGroupId, Long toGroupId, Long userId) {
        if (fromGroupId.equals(toGroupId)) {
            throw new BusinessException(ErrorCode.GROUP_SAME_GROUP);
        }

        RoundGroup fromGroup = this.findGroupById(fromGroupId);
        RoundGroup toGroup = this.findGroupById(toGroupId);

        if (!fromGroup.getRoundId().equals(toGroup.getRoundId())) {
            throw new BusinessException(ErrorCode.GROUP_CHANGE_DIFFERENT_ROUND);
        }

        Round round = this.findRoundById(fromGroup.getRoundId());
        round.verifyOpen();
        OrgMember orgMember = this.resolveOrgMember(round.getSessionId(), userId);

        // from 그룹 참여 확인
        RoundGroupMember existingMember = this.roundGroupMemberRepository
                .findByGroupIdAndOrgMemberId(fromGroupId, orgMember.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_JOINED));

        // to 그룹 중복 참여 확인
        if (this.roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(toGroupId, orgMember.getId())) {
            throw new BusinessException(ErrorCode.GROUP_ALREADY_JOINED);
        }

        // 기존 멤버십 hard delete (uk_group_member UNIQUE 제약 재사용 허용)
        this.roundGroupMemberRepository.delete(existingMember);

        // 새 멤버십 생성
        RoundGroupMember newMember = RoundGroupMember.builder()
                .groupId(toGroupId)
                .orgMemberId(orgMember.getId())
                .build();
        this.roundGroupMemberRepository.save(newMember);
    }

    /**
     * 그룹의 공유 메뉴를 전체 교체 방식으로 저장한다.
     *
     * <p>루프 내 findById 반복 호출을 제거하고 findAllById 단일 배치 조회로 전환하여
     * N+1 쿼리를 방지한다.</p>
     *
     * @param groupId 그룹 ID
     * @param request 공유 메뉴 저장 요청
     */
    @Transactional
    public void saveSharedMenus(Long groupId, SharedMenuSaveRequest request) {
        RoundGroup group = this.findGroupById(groupId);
        this.findRoundById(group.getRoundId()).verifyOpen();

        // 기존 공유 메뉴 전체 soft delete
        List<RoundGroupSharedMenu> existingMenus = this.roundGroupSharedMenuRepository.findByGroupId(groupId);
        existingMenus.forEach(RoundGroupSharedMenu::delete);

        // 요청된 메뉴 ID를 한 번에 일괄 조회하여 N+1 방지
        List<Long> menuIds = request.getMenus().stream()
                .map(SharedMenuSaveRequest.SharedMenuItem::getMenuId)
                .collect(Collectors.toList());

        Map<Long, StoreMenu> storeMenuMap = this.storeMenuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(StoreMenu::getId, Function.identity()));

        // 존재하지 않는 메뉴 ID 검증
        menuIds.forEach(menuId -> {
            if (!storeMenuMap.containsKey(menuId)) {
                throw new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND);
            }
        });

        // 새 공유 메뉴 생성
        for (SharedMenuSaveRequest.SharedMenuItem item : request.getMenus()) {
            RoundGroupSharedMenu sharedMenu = RoundGroupSharedMenu.builder()
                    .groupId(groupId)
                    .menuId(item.getMenuId())
                    .quantity(item.getQuantity())
                    .build();
            this.roundGroupSharedMenuRepository.save(sharedMenu);
        }
    }

    /**
     * 하위 그룹을 인메모리에서 재귀적으로 수집한다.
     *
     * <p>미리 조회한 childrenMap을 사용하여 N+1 쿼리를 방지한다.</p>
     *
     * @param group       현재 그룹
     * @param collected   수집된 그룹 목록
     * @param childrenMap parentGroupId → 자식 그룹 목록 맵
     */
    private void collectDescendantsInMemory(RoundGroup group, List<RoundGroup> collected,
                                             Map<Long, List<RoundGroup>> childrenMap) {
        collected.add(group);
        List<RoundGroup> children = childrenMap.getOrDefault(group.getId(), List.of());
        for (RoundGroup child : children) {
            this.collectDescendantsInMemory(child, collected, childrenMap);
        }
    }

    /**
     * 단일 그룹의 응답을 빌드한다. (수정 시 사용)
     *
     * <p>N+1 쿼리를 방지하기 위해 라운드에 속한 모든 그룹·멤버·공유 메뉴·주문 데이터를
     * 배치로 조회한 뒤, 인메모리에서 대상 그룹 기준의 서브트리를 조합한다.
     * DB 쿼리 횟수는 트리 깊이/너비에 무관하게 O(1)이다.</p>
     *
     * @param group  그룹 엔티티
     * @param userId 현재 사용자 ID
     * @return 그룹 응답 (하위 그룹 포함 서브트리)
     */
    private GroupResponse buildSingleGroupResponse(RoundGroup group, Long userId) {
        Round round = this.findRoundById(group.getRoundId());

        OrgMember currentOrgMember = this.orgMemberRepository
                .findBySessionIdAndUserId(round.getSessionId(), userId)
                .orElse(null);
        Long currentOrgMemberId = currentOrgMember != null ? currentOrgMember.getId() : null;

        // 1. 배치 페치: 라운드 내 모든 그룹
        List<RoundGroup> allGroups = this.roundGroupRepository.findByRoundId(group.getRoundId());
        List<Long> allGroupIds = allGroups.stream().map(RoundGroup::getId).toList();

        // 2. 배치 페치: 멤버, 공유 메뉴
        List<RoundGroupMember> allGroupMembers = this.roundGroupMemberRepository.findByGroupIdIn(allGroupIds);
        List<RoundGroupSharedMenu> allSharedMenus = this.roundGroupSharedMenuRepository.findByGroupIdIn(allGroupIds);

        // 3. 스토어 메뉴 배치 조회
        List<StoreMenu> storeMenus = this.storeMenuRepository.findAllByStoreId(round.getStoreId());
        Map<Long, StoreMenu> menuMap = storeMenus.stream()
                .collect(Collectors.toMap(StoreMenu::getId, Function.identity()));

        // 4. OrgMember 배치 조회
        List<Long> orgMemberIds = allGroupMembers.stream()
                .map(RoundGroupMember::getOrgMemberId).distinct().toList();
        Map<Long, OrgMember> orgMemberMap = this.orgMemberRepository.findAllById(orgMemberIds).stream()
                .collect(Collectors.toMap(OrgMember::getId, Function.identity()));

        // 5. 주문/주문상세 배치 조회
        List<Order> orders = this.orderRepository.findByRoundId(group.getRoundId());
        Map<Long, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<OrderDetail> allOrderDetails = List.of();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(Order::getId).toList();
            allOrderDetails = this.orderDetailRepository.findByOrderIdIn(orderIds);
        }
        Map<Long, List<OrderDetail>> detailsByOrgMemberId = allOrderDetails.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrgMemberId));

        // 6. 그룹별 멤버·공유 메뉴 맵 빌드
        Map<Long, List<RoundGroupMember>> membersByGroupId = allGroupMembers.stream()
                .collect(Collectors.groupingBy(RoundGroupMember::getGroupId));
        Map<Long, List<RoundGroupSharedMenu>> sharedMenusByGroupId = allSharedMenus.stream()
                .collect(Collectors.groupingBy(RoundGroupSharedMenu::getGroupId));

        // 7. 인메모리 트리 조합: 대상 그룹 기준 서브트리만 반환
        Map<Long, GroupResponse> responseMap = new LinkedHashMap<>();
        for (RoundGroup g : allGroups) {
            List<SharedMenuResponse> sharedMenuResponses = sharedMenusByGroupId
                    .getOrDefault(g.getId(), List.of()).stream()
                    .map(sm -> {
                        StoreMenu storeMenu = menuMap.get(sm.getMenuId());
                        return SharedMenuResponse.builder()
                                .menuId(sm.getMenuId())
                                .menuName(storeMenu != null ? storeMenu.getName() : "")
                                .price(storeMenu != null ? storeMenu.getPrice() : BigDecimal.ZERO)
                                .quantity(sm.getQuantity())
                                .build();
                    })
                    .toList();

            List<GroupMemberResponse> memberResponses = membersByGroupId
                    .getOrDefault(g.getId(), List.of()).stream()
                    .map(gm -> {
                        OrgMember orgMember = orgMemberMap.get(gm.getOrgMemberId());
                        List<PersonalOrderResponse> personalOrders = this.buildPersonalOrders(
                                gm.getOrgMemberId(), detailsByOrgMemberId, orderMap);
                        BigDecimal memberTotal = personalOrders.stream()
                                .map(PersonalOrderResponse::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        return GroupMemberResponse.builder()
                                .orgMemberId(gm.getOrgMemberId())
                                .nickname(orgMember != null ? orgMember.getNickname() : "")
                                .isCurrentUser(gm.getOrgMemberId().equals(currentOrgMemberId))
                                .personalOrders(personalOrders)
                                .totalAmount(memberTotal)
                                .build();
                    })
                    .toList();

            boolean isParticipating = membersByGroupId
                    .getOrDefault(g.getId(), List.of()).stream()
                    .anyMatch(gm -> gm.getOrgMemberId().equals(currentOrgMemberId));

            GroupResponse response = GroupResponse.builder()
                    .groupId(g.getId())
                    .groupName(g.getName())
                    .parentGroupId(g.getParentGroupId())
                    .depth(g.getDepth())
                    .isParticipating(isParticipating)
                    .sharedMenus(sharedMenuResponses)
                    .members(memberResponses)
                    .childGroups(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .build();

            responseMap.put(g.getId(), response);
        }

        // 8. flat → tree 연결 (부모-자식 링크)
        for (GroupResponse response : responseMap.values()) {
            if (response.getParentGroupId() != null) {
                GroupResponse parent = responseMap.get(response.getParentGroupId());
                if (parent != null) {
                    parent.getChildGroups().add(response);
                }
            }
        }

        // 9. 대상 그룹 루트로 totalAmount 재귀 계산 (bottom-up)
        GroupResponse targetResponse = responseMap.get(group.getId());
        this.calculateTotalAmount(targetResponse);

        return targetResponse;
    }

    /**
     * 멤버의 개인 주문 응답 목록을 빌드한다.
     *
     * <p>주문 시점에 스냅샷으로 저장된 {@code menuName}, {@code menuPrice}를 사용하므로
     * StoreMenu 조회 없이 주문 데이터만으로 응답을 구성한다.</p>
     *
     * @param orgMemberId          조직 멤버 ID
     * @param detailsByOrgMemberId orgMemberId별 주문 상세 맵
     * @param orderMap             주문 맵
     * @return 개인 주문 응답 목록
     */
    private List<PersonalOrderResponse> buildPersonalOrders(
            Long orgMemberId,
            Map<Long, List<OrderDetail>> detailsByOrgMemberId,
            Map<Long, Order> orderMap) {

        List<OrderDetail> details = detailsByOrgMemberId.getOrDefault(orgMemberId, List.of());

        return details.stream()
                .map(detail -> {
                    Order order = orderMap.get(detail.getOrderId());
                    if (order == null) {
                        return null;
                    }
                    BigDecimal totalAmount = order.getMenuPrice().multiply(BigDecimal.valueOf(order.getQuantity()));

                    return PersonalOrderResponse.builder()
                            .menuId(order.getMenuId())
                            .menuName(order.getMenuName())
                            .price(order.getMenuPrice())
                            .quantity(order.getQuantity())
                            .totalAmount(totalAmount)
                            .build();
                })
                .filter(p -> p != null)
                .toList();
    }

    /**
     * 그룹 트리의 totalAmount를 재귀적으로 계산한다. (bottom-up)
     *
     * <p>공유 메뉴 합계 + 멤버 개인 주문 합계 + 모든 자식 그룹 합계를 합산한다.
     * 자식 그룹의 재귀 반환값을 사용하여 부모에 정확히 합산한다.</p>
     *
     * @param group 그룹 응답
     * @return 해당 그룹의 totalAmount (공유 메뉴 + 멤버 + 자식 그룹 전체 합계)
     */
    private BigDecimal calculateTotalAmount(GroupResponse group) {
        // 공유 메뉴 합계
        BigDecimal sharedMenuTotal = group.getSharedMenus().stream()
                .map(sm -> sm.getPrice().multiply(BigDecimal.valueOf(sm.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 멤버 개인 주문 합계
        BigDecimal membersTotal = group.getMembers().stream()
                .map(GroupMemberResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 자식 그룹 합계 (재귀 반환값을 합산)
        BigDecimal childrenTotal = group.getChildGroups().stream()
                .map(this::calculateTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = sharedMenuTotal.add(membersTotal).add(childrenTotal);
        group.setTotalAmount(total);
        return total;
    }

    /**
     * 세션에서 사용자에 해당하는 OrgMember를 조회하거나 자동 생성한다.
     *
     * <p>조회 우선순위:
     * <ol>
     *   <li>userId로 직접 연결된 OrgMember 조회</li>
     *   <li>Member.name과 닉네임이 일치하는 미연결 OrgMember 자동 연결</li>
     *   <li>세션에 새 OrgMember를 자동 생성</li>
     * </ol></p>
     *
     * @param sessionId 세션 ID
     * @param userId    사용자 ID
     * @return 조직 멤버
     * @throws EntityNotFoundException Member를 찾을 수 없는 경우
     */
    private OrgMember resolveOrgMember(Long sessionId, Long userId) {
        return this.orgMemberRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseGet(() -> {
                    Member member = this.memberRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));

                    // 닉네임 매칭으로 미연결 OrgMember 자동 연결 시도
                    return this.orgMemberRepository
                            .findBySessionIdAndNicknameAndUserIdIsNull(sessionId, member.getName())
                            .map(unlinked -> {
                                unlinked.linkUser(userId);
                                return unlinked;
                            })
                            .orElseGet(() -> {
                                // 매칭 실패 시 세션에 새 OrgMember 자동 생성
                                OrgMember newOrgMember = OrgMember.builder()
                                        .sessionId(sessionId)
                                        .userId(userId)
                                        .nickname(member.getName())
                                        .isActive(true)
                                        .build();
                                return this.orgMemberRepository.save(newOrgMember);
                            });
                });
    }

    private RoundGroup findGroupById(Long groupId) {
        return this.roundGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.GROUP_NOT_FOUND));
    }

    private Round findRoundById(Long roundId) {
        return this.roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ROUND_NOT_FOUND));
    }

}
