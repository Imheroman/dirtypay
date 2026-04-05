package com.dirtypay.domain.group.service;

import com.dirtypay.domain.group.dto.request.GroupCreateRequest;
import com.dirtypay.domain.group.dto.request.GroupUpdateRequest;
import com.dirtypay.domain.group.dto.request.SharedMenuSaveRequest;
import com.dirtypay.domain.group.dto.response.GroupResponse;
import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
import com.dirtypay.domain.group.entity.RoundGroupSharedMenu;
import com.dirtypay.domain.group.repository.RoundGroupMemberRepository;
import com.dirtypay.domain.group.repository.RoundGroupRepository;
import com.dirtypay.domain.group.repository.RoundGroupSharedMenuRepository;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.order.entity.Order;
import com.dirtypay.domain.order.entity.OrderDetail;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.order.repository.OrderDetailRepository;
import com.dirtypay.domain.order.repository.OrderRepository;
import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.domain.organization.repository.OrgMemberRepository;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * {@link GroupService} 단위 테스트.
 *
 * <p>BDDMockito + AssertJ 기반 given-when-then 패턴을 사용한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private RoundGroupRepository roundGroupRepository;

    @Mock
    private RoundGroupMemberRepository roundGroupMemberRepository;

    @Mock
    private RoundGroupSharedMenuRepository roundGroupSharedMenuRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundParticipantRepository roundParticipantRepository;

    @Mock
    private OrgMemberRepository orgMemberRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Nested
    @DisplayName("createGroup 테스트")
    class CreateGroupTest {

        @Test
        @DisplayName("루트 그룹 생성 성공 - parentGroupId가 null이면 depth 0으로 생성된다")
        void createGroup_rootGroup_success() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;

            Round round = createRound(roundId, sessionId);

            GroupCreateRequest request = new GroupCreateRequest();
            ReflectionTestUtils.setField(request, "name", "1조");
            ReflectionTestUtils.setField(request, "parentGroupId", null);

            RoundGroup savedGroup = createRoundGroup(200L, roundId, null, "1조", 0);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.save(any(RoundGroup.class))).willReturn(savedGroup);

            // when
            GroupResponse response = groupService.createGroup(roundId, request, userId);

            // then
            assertThat(response.getGroupId()).isEqualTo(200L);
            assertThat(response.getGroupName()).isEqualTo("1조");
            assertThat(response.getParentGroupId()).isNull();
            assertThat(response.getDepth()).isZero();
            assertThat(response.isParticipating()).isFalse();
            assertThat(response.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("하위 그룹 생성 성공 - parentGroupId가 지정되면 부모 depth + 1로 생성된다")
        void createGroup_nestedGroup_success() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;
            Long parentGroupId = 200L;

            Round round = createRound(roundId, sessionId);
            RoundGroup parentGroup = createRoundGroup(parentGroupId, roundId, null, "부모그룹", 0);

            GroupCreateRequest request = new GroupCreateRequest();
            ReflectionTestUtils.setField(request, "name", "하위그룹");
            ReflectionTestUtils.setField(request, "parentGroupId", parentGroupId);

            RoundGroup savedGroup = createRoundGroup(201L, roundId, parentGroupId, "하위그룹", 1);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findById(parentGroupId)).willReturn(Optional.of(parentGroup));
            given(roundGroupRepository.save(any(RoundGroup.class))).willReturn(savedGroup);

            // when
            GroupResponse response = groupService.createGroup(roundId, request, userId);

            // then
            assertThat(response.getGroupId()).isEqualTo(201L);
            assertThat(response.getParentGroupId()).isEqualTo(parentGroupId);
            assertThat(response.getDepth()).isEqualTo(1);
            assertThat(response.isParticipating()).isFalse();
            assertThat(response.getMembers()).isEmpty();
        }

        @Test
        @DisplayName("부모 그룹이 다른 라운드에 속하면 GROUP_NOT_FOUND 예외가 발생한다")
        void createGroup_parentFromDifferentRound_throwsException() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;
            Long parentGroupId = 200L;

            Round round = createRound(roundId, sessionId);
            RoundGroup parentGroup = createRoundGroup(parentGroupId, 999L, null, "다른라운드그룹", 0);

            GroupCreateRequest request = new GroupCreateRequest();
            ReflectionTestUtils.setField(request, "name", "하위그룹");
            ReflectionTestUtils.setField(request, "parentGroupId", parentGroupId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findById(parentGroupId)).willReturn(Optional.of(parentGroup));

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(roundId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_ROUND_MISMATCH);
        }
    }

    @Nested
    @DisplayName("updateGroup 테스트")
    class UpdateGroupTest {

        @Test
        @DisplayName("그룹명 수정 성공")
        void updateGroup_success() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "기존이름", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");

            GroupUpdateRequest request = new GroupUpdateRequest();
            ReflectionTestUtils.setField(request, "name", "변경된이름");

            // buildSingleGroupResponse 내 배치 페치 스텁
            // 새 구현은 findByRoundId로 모든 그룹을 한 번에 조회한 뒤 인메모리 트리 조합
            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId))).willReturn(List.of());
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(groupId))).willReturn(List.of());
            given(storeMenuRepository.findAllByStoreId(any())).willReturn(List.of());
            given(orgMemberRepository.findAllById(List.of())).willReturn(List.of());
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of());

            // when
            GroupResponse response = groupService.updateGroup(groupId, request, userId);

            // then
            assertThat(response.getGroupName()).isEqualTo("변경된이름");
            assertThat(response.getGroupId()).isEqualTo(groupId);
        }
    }

    @Nested
    @DisplayName("deleteGroup 테스트")
    class DeleteGroupTest {

        @Test
        @DisplayName("리프 노드 삭제 성공 - 멤버가 없으면 공유 메뉴와 그룹이 soft delete 된다")
        void deleteGroup_leafNode_success() {
            // given
            Long groupId = 200L;
            Long roundId = 1L;
            RoundGroup group = createRoundGroup(groupId, roundId, null, "리프그룹", 0);
            Round round = createRound(roundId, 10L);

            RoundGroupSharedMenu sharedMenu = createRoundGroupSharedMenu(1L, groupId, 10L, 2);

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId))).willReturn(List.of());
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(groupId))).willReturn(List.of(sharedMenu));

            // when
            groupService.deleteGroup(groupId);

            // then
            assertThat(group.isDeleted()).isTrue();
            assertThat(sharedMenu.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("하위 그룹 포함 삭제 - 멤버가 없으면 자손 그룹과 공유 메뉴가 모두 soft delete 된다")
        void deleteGroup_withDescendants_cascadingSoftDelete() {
            // given
            Long parentGroupId = 200L;
            Long childGroupId = 201L;
            Long roundId = 1L;

            RoundGroup parentGroup = createRoundGroup(parentGroupId, roundId, null, "부모그룹", 0);
            RoundGroup childGroup = createRoundGroup(childGroupId, roundId, parentGroupId, "자식그룹", 1);
            Round round = createRound(roundId, 10L);

            RoundGroupSharedMenu parentMenu = createRoundGroupSharedMenu(1L, parentGroupId, 10L, 1);
            RoundGroupSharedMenu childMenu = createRoundGroupSharedMenu(2L, childGroupId, 11L, 3);

            given(roundGroupRepository.findById(parentGroupId)).willReturn(Optional.of(parentGroup));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(parentGroup, childGroup));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of());
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of(parentMenu, childMenu));

            // when
            groupService.deleteGroup(parentGroupId);

            // then
            assertThat(parentGroup.isDeleted()).isTrue();
            assertThat(childGroup.isDeleted()).isTrue();
            assertThat(parentMenu.isDeleted()).isTrue();
            assertThat(childMenu.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("참여 중인 멤버가 있으면 GROUP_HAS_MEMBERS 예외가 발생한다")
        void deleteGroup_hasMembers_throwsException() {
            // given
            Long groupId = 200L;
            Long roundId = 1L;
            RoundGroup group = createRoundGroup(groupId, roundId, null, "그룹", 0);
            Round round = createRound(roundId, 10L);

            RoundGroupMember member = createRoundGroupMember(1L, groupId, 50L);

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId))).willReturn(List.of(member));

            // when & then
            assertThatThrownBy(() -> groupService.deleteGroup(groupId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_HAS_MEMBERS);

            assertThat(group.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("joinGroup 테스트")
    class JoinGroupTest {

        @Test
        @DisplayName("그룹 참여 성공 - 기존 OrgMember가 존재하면 바로 참여한다")
        void joinGroup_success() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, 50L))
                    .willReturn(false);
            given(roundGroupMemberRepository.saveAndFlush(any(RoundGroupMember.class)))
                    .willReturn(createRoundGroupMember(1L, groupId, 50L));

            // when
            groupService.joinGroup(groupId, userId);

            // then
            then(roundGroupMemberRepository).should().saveAndFlush(any(RoundGroupMember.class));
        }

        @Test
        @DisplayName("이미 참여한 그룹에 재참여하면 GROUP_ALREADY_JOINED 예외가 발생한다")
        void joinGroup_alreadyJoined_throwsException() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, 50L))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> groupService.joinGroup(groupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_ALREADY_JOINED);

            then(roundGroupMemberRepository).should(never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("OrgMember 자동 생성 - 닉네임 매칭 실패 시 루트 노드에 새 OrgMember가 생성된다")
        void joinGroup_resolveOrgMember_autoCreate() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            Member member = createMember(userId, "김철수");
            OrgMember newOrgMember = createOrgMember(60L, sessionId, userId, "김철수");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.empty());
            given(memberRepository.findById(userId)).willReturn(Optional.of(member));
            given(orgMemberRepository.findBySessionIdAndNicknameAndUserIdIsNull(sessionId, "김철수"))
                    .willReturn(Optional.empty());
            given(orgMemberRepository.save(any(OrgMember.class))).willReturn(newOrgMember);
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, 60L))
                    .willReturn(false);
            given(roundGroupMemberRepository.saveAndFlush(any(RoundGroupMember.class)))
                    .willReturn(createRoundGroupMember(1L, groupId, 60L));

            // when
            groupService.joinGroup(groupId, userId);

            // then
            then(orgMemberRepository).should().save(any(OrgMember.class));
            then(roundGroupMemberRepository).should().saveAndFlush(any(RoundGroupMember.class));
        }

        @Test
        @DisplayName("saveAndFlush 시 DataIntegrityViolationException 발생 → GROUP_ALREADY_JOINED 예외로 변환된다")
        void joinGroup_dataIntegrityViolation_throwsGroupAlreadyJoined() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, 50L))
                    .willReturn(false);
            given(roundGroupMemberRepository.existsByRoundIdAndOrgMemberId(roundId, 50L))
                    .willReturn(false);
            given(roundGroupMemberRepository.saveAndFlush(any(RoundGroupMember.class)))
                    .willThrow(new DataIntegrityViolationException("uk_group_member"));

            // when & then
            // DB UNIQUE 제약 위반이 GROUP_ALREADY_JOINED 비즈니스 예외로 변환되어야 한다
            assertThatThrownBy(() -> groupService.joinGroup(groupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_ALREADY_JOINED);
        }

        @Test
        @DisplayName("OrgMember 닉네임 매칭 - 미연결 OrgMember가 자동 연결된다")
        void joinGroup_resolveOrgMember_nicknameMatch() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            Member member = createMember(userId, "홍길동");
            OrgMember unlinkedOrgMember = createOrgMember(55L, 1L, null, "홍길동");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.empty());
            given(memberRepository.findById(userId)).willReturn(Optional.of(member));
            given(orgMemberRepository.findBySessionIdAndNicknameAndUserIdIsNull(sessionId, "홍길동"))
                    .willReturn(Optional.of(unlinkedOrgMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(groupId, 55L))
                    .willReturn(false);
            given(roundGroupMemberRepository.saveAndFlush(any(RoundGroupMember.class)))
                    .willReturn(createRoundGroupMember(1L, groupId, 55L));

            // when
            groupService.joinGroup(groupId, userId);

            // then
            assertThat(unlinkedOrgMember.getUserId()).isEqualTo(userId);
            then(roundGroupMemberRepository).should().saveAndFlush(any(RoundGroupMember.class));
        }
    }

    @Nested
    @DisplayName("leaveGroup 테스트")
    class LeaveGroupTest {

        @Test
        @DisplayName("그룹 탈퇴 성공 - 참여 중인 멤버가 삭제된다")
        void leaveGroup_success() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");
            RoundGroupMember groupMember = createRoundGroupMember(1L, groupId, 50L);

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.findByGroupIdAndOrgMemberId(groupId, 50L))
                    .willReturn(Optional.of(groupMember));

            // when
            groupService.leaveGroup(groupId, userId);

            // then — uk_group_member UNIQUE 제약 재사용을 위해 hard delete
            then(roundGroupMemberRepository).should(times(1)).delete(groupMember);
        }

        @Test
        @DisplayName("참여하지 않은 그룹에서 탈퇴하면 GROUP_NOT_JOINED 예외가 발생한다")
        void leaveGroup_notJoined_throwsException() {
            // given
            Long groupId = 200L;
            Long userId = 100L;
            Long roundId = 1L;
            Long sessionId = 10L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(50L, 1L, userId, "홍길동");

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.findByGroupIdAndOrgMemberId(groupId, 50L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.leaveGroup(groupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_NOT_JOINED);
        }
    }

    @Nested
    @DisplayName("changeGroup 테스트")
    class ChangeGroupTest {

        @Test
        @DisplayName("그룹 이동 성공 - from 그룹에서 to 그룹으로 멤버십 이동")
        void changeGroup_success() {
            // given
            Long fromGroupId = 100L;
            Long toGroupId = 101L;
            Long userId = 1L;
            Long orgMemberId = 50L;
            Long roundId = 10L;
            Long sessionId = 5L;

            RoundGroup fromGroup = createRoundGroup(fromGroupId, roundId, null, "1조", 0);
            RoundGroup toGroup = createRoundGroup(toGroupId, roundId, null, "2조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(orgMemberId, 1L, userId, "닉네임");
            RoundGroupMember existingMember = new RoundGroupMember(fromGroupId, orgMemberId);
            ReflectionTestUtils.setField(existingMember, "id", 1L);

            given(roundGroupRepository.findById(fromGroupId)).willReturn(Optional.of(fromGroup));
            given(roundGroupRepository.findById(toGroupId)).willReturn(Optional.of(toGroup));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.findByGroupIdAndOrgMemberId(fromGroupId, orgMemberId))
                    .willReturn(Optional.of(existingMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(toGroupId, orgMemberId))
                    .willReturn(false);

            // when
            groupService.changeGroup(fromGroupId, toGroupId, userId);

            // then — 기존 멤버십은 hard delete, 새 멤버십은 save
            then(roundGroupMemberRepository).should(times(1)).delete(existingMember);
            then(roundGroupMemberRepository).should(times(1)).save(any(RoundGroupMember.class));
        }

        @Test
        @DisplayName("같은 그룹으로 이동 시도 시 GROUP_SAME_GROUP 예외")
        void changeGroup_sameGroup_throwsException() {
            // given
            Long groupId = 100L;
            Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> groupService.changeGroup(groupId, groupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_SAME_GROUP);
        }

        @Test
        @DisplayName("다른 라운드의 그룹으로 이동 시 GROUP_NOT_FOUND 예외")
        void changeGroup_differentRound_throwsException() {
            // given
            Long fromGroupId = 100L;
            Long toGroupId = 101L;
            Long userId = 1L;

            RoundGroup fromGroup = createRoundGroup(fromGroupId, 10L, null, "1조", 0);
            RoundGroup toGroup = createRoundGroup(toGroupId, 20L, null, "2조", 0);

            given(roundGroupRepository.findById(fromGroupId)).willReturn(Optional.of(fromGroup));
            given(roundGroupRepository.findById(toGroupId)).willReturn(Optional.of(toGroup));

            // when & then
            assertThatThrownBy(() -> groupService.changeGroup(fromGroupId, toGroupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_CHANGE_DIFFERENT_ROUND);
        }

        @Test
        @DisplayName("라운드가 닫혀있을 때 그룹 이동 불가")
        void changeGroup_closedRound_throwsException() {
            // given
            Long fromGroupId = 100L;
            Long toGroupId = 101L;
            Long userId = 1L;
            Long roundId = 10L;

            RoundGroup fromGroup = createRoundGroup(fromGroupId, roundId, null, "1조", 0);
            RoundGroup toGroup = createRoundGroup(toGroupId, roundId, null, "2조", 0);
            Round closedRound = createRound(roundId, 5L);
            ReflectionTestUtils.setField(closedRound, "status", RoundStatus.CLOSED);

            given(roundGroupRepository.findById(fromGroupId)).willReturn(Optional.of(fromGroup));
            given(roundGroupRepository.findById(toGroupId)).willReturn(Optional.of(toGroup));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(closedRound));

            // when & then
            assertThatThrownBy(() -> groupService.changeGroup(fromGroupId, toGroupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_ALREADY_CLOSED);
        }

        @Test
        @DisplayName("from 그룹에 참여하지 않은 경우 GROUP_NOT_JOINED 예외")
        void changeGroup_notJoinedFromGroup_throwsException() {
            // given
            Long fromGroupId = 100L;
            Long toGroupId = 101L;
            Long userId = 1L;
            Long orgMemberId = 50L;
            Long roundId = 10L;
            Long sessionId = 5L;

            RoundGroup fromGroup = createRoundGroup(fromGroupId, roundId, null, "1조", 0);
            RoundGroup toGroup = createRoundGroup(toGroupId, roundId, null, "2조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(orgMemberId, 1L, userId, "닉네임");

            given(roundGroupRepository.findById(fromGroupId)).willReturn(Optional.of(fromGroup));
            given(roundGroupRepository.findById(toGroupId)).willReturn(Optional.of(toGroup));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.findByGroupIdAndOrgMemberId(fromGroupId, orgMemberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.changeGroup(fromGroupId, toGroupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_NOT_JOINED);
        }

        @Test
        @DisplayName("to 그룹에 이미 참여한 경우 GROUP_ALREADY_JOINED 예외")
        void changeGroup_alreadyJoinedToGroup_throwsException() {
            // given
            Long fromGroupId = 100L;
            Long toGroupId = 101L;
            Long userId = 1L;
            Long orgMemberId = 50L;
            Long roundId = 10L;
            Long sessionId = 5L;

            RoundGroup fromGroup = createRoundGroup(fromGroupId, roundId, null, "1조", 0);
            RoundGroup toGroup = createRoundGroup(toGroupId, roundId, null, "2조", 0);
            Round round = createRound(roundId, sessionId);
            OrgMember orgMember = createOrgMember(orgMemberId, 1L, userId, "닉네임");
            RoundGroupMember existingMember = new RoundGroupMember(fromGroupId, orgMemberId);

            given(roundGroupRepository.findById(fromGroupId)).willReturn(Optional.of(fromGroup));
            given(roundGroupRepository.findById(toGroupId)).willReturn(Optional.of(toGroup));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(orgMember));
            given(roundGroupMemberRepository.findByGroupIdAndOrgMemberId(fromGroupId, orgMemberId))
                    .willReturn(Optional.of(existingMember));
            given(roundGroupMemberRepository.existsByGroupIdAndOrgMemberId(toGroupId, orgMemberId))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> groupService.changeGroup(fromGroupId, toGroupId, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GROUP_ALREADY_JOINED);
        }
    }

    @Nested
    @DisplayName("saveSharedMenus 테스트")
    class SaveSharedMenusTest {

        @Test
        @DisplayName("공유 메뉴 저장 성공 - 기존 메뉴가 삭제되고 새 메뉴가 저장된다")
        void saveSharedMenus_replaceOldWithNew_success() {
            // given
            Long groupId = 200L;
            Long roundId = 1L;
            Long menuId1 = 10L;
            Long menuId2 = 11L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, 10L);
            RoundGroupSharedMenu existingMenu = createRoundGroupSharedMenu(1L, groupId, 5L, 1);
            StoreMenu storeMenu1 = createStoreMenu(menuId1, "피자", new BigDecimal("15000"));
            StoreMenu storeMenu2 = createStoreMenu(menuId2, "콜라", new BigDecimal("2000"));

            SharedMenuSaveRequest request = new SharedMenuSaveRequest();
            SharedMenuSaveRequest.SharedMenuItem item1 = new SharedMenuSaveRequest.SharedMenuItem();
            ReflectionTestUtils.setField(item1, "menuId", menuId1);
            ReflectionTestUtils.setField(item1, "quantity", 2);
            SharedMenuSaveRequest.SharedMenuItem item2 = new SharedMenuSaveRequest.SharedMenuItem();
            ReflectionTestUtils.setField(item2, "menuId", menuId2);
            ReflectionTestUtils.setField(item2, "quantity", 3);
            ReflectionTestUtils.setField(request, "menus", List.of(item1, item2));

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupSharedMenuRepository.findByGroupId(groupId)).willReturn(List.of(existingMenu));
            given(storeMenuRepository.findAllById(List.of(menuId1, menuId2)))
                    .willReturn(List.of(storeMenu1, storeMenu2));
            given(roundGroupSharedMenuRepository.save(any(RoundGroupSharedMenu.class)))
                    .willReturn(createRoundGroupSharedMenu(2L, groupId, menuId1, 2));

            // when
            groupService.saveSharedMenus(groupId, request);

            // then
            assertThat(existingMenu.isDeleted()).isTrue();
            then(roundGroupSharedMenuRepository).should(times(2)).save(any(RoundGroupSharedMenu.class));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 ID가 포함되면 MENU_NOT_FOUND 예외가 발생한다")
        void saveSharedMenus_menuNotFound_throwsException() {
            // given
            Long groupId = 200L;
            Long roundId = 1L;
            Long invalidMenuId = 999L;

            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            Round round = createRound(roundId, 10L);

            SharedMenuSaveRequest request = new SharedMenuSaveRequest();
            SharedMenuSaveRequest.SharedMenuItem item = new SharedMenuSaveRequest.SharedMenuItem();
            ReflectionTestUtils.setField(item, "menuId", invalidMenuId);
            ReflectionTestUtils.setField(item, "quantity", 1);
            ReflectionTestUtils.setField(request, "menus", List.of(item));

            given(roundGroupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(roundGroupSharedMenuRepository.findByGroupId(groupId)).willReturn(List.of());
            given(storeMenuRepository.findAllById(List.of(invalidMenuId))).willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> groupService.saveSharedMenus(groupId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getGroups 테스트")
    class GetGroupsTest {

        @Test
        @DisplayName("그룹이 없는 라운드 조회 시 빈 리스트를 반환한다")
        void getGroups_emptyRound_returnsEmptyList() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;

            Round round = createRound(roundId, sessionId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.empty());
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of());

            // when
            List<GroupResponse> responses = groupService.getGroups(roundId, userId);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("멤버가 포함된 단일 그룹 조회 성공")
        void getGroups_singleGroupWithMembers_success() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;
            Long groupId = 200L;
            Long orgMemberId = 50L;

            Round round = createRound(roundId, sessionId);
            OrgMember currentOrgMember = createOrgMember(orgMemberId, 1L, userId, "홍길동");
            RoundGroup group = createRoundGroup(groupId, roundId, null, "1조", 0);
            RoundGroupMember groupMember = createRoundGroupMember(1L, groupId, orgMemberId);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.of(currentOrgMember));
            given(roundGroupRepository.findByRoundId(roundId)).willReturn(List.of(group));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of(groupMember));
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(groupId)))
                    .willReturn(List.of());
            given(storeMenuRepository.findAllByStoreId(any())).willReturn(List.of());
            given(orgMemberRepository.findAllById(List.of(orgMemberId)))
                    .willReturn(List.of(currentOrgMember));
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of());

            // when
            List<GroupResponse> responses = groupService.getGroups(roundId, userId);

            // then
            assertThat(responses).hasSize(1);
            GroupResponse response = responses.get(0);
            assertThat(response.getGroupId()).isEqualTo(groupId);
            assertThat(response.getGroupName()).isEqualTo("1조");
            assertThat(response.isParticipating()).isTrue();
            assertThat(response.getMembers()).hasSize(1);
            assertThat(response.getMembers().get(0).getNickname()).isEqualTo("홍길동");
            assertThat(response.getMembers().get(0).isCurrentUser()).isTrue();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("calculateTotalAmount 합산 테스트")
    class CalculateTotalAmountTest {

        /**
         * 자식 그룹의 totalAmount가 부모 그룹에 합산되는지 검증한다.
         *
         * <p>구조:
         * <ul>
         *   <li>부모 그룹 (ID=200): 멤버 없음, 공유 메뉴 없음</li>
         *   <li>자식 그룹 (ID=201): orgMember(ID=51)가 10,000원 주문</li>
         * </ul>
         * 기대: 부모 totalAmount = 10,000원 (자식 totalAmount 포함)</p>
         */
        @Test
        @DisplayName("자식 그룹 금액이 부모 totalAmount에 합산된다")
        void getGroups_childAmountRollsUpToParent() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;
            Long parentGroupId = 200L;
            Long childGroupId = 201L;
            Long childOrgMemberId = 51L;
            Long orderId = 1001L;

            Round round = createRound(roundId, sessionId);
            RoundGroup parentGroup = createRoundGroup(parentGroupId, roundId, null, "부모그룹", 0);
            RoundGroup childGroup = createRoundGroup(childGroupId, roundId, parentGroupId, "자식그룹", 1);
            RoundGroupMember childMember = createRoundGroupMember(2L, childGroupId, childOrgMemberId);
            OrgMember childOrgMember = createOrgMember(childOrgMemberId, sessionId, null, "김철수");

            // 자식 멤버의 주문: 10,000원 x 1개
            Order order = Order.builder()
                    .roundId(roundId)
                    .groupId(childGroupId)
                    .groupName("자식그룹")
                    .menuId(10L)
                    .menuName("피자")
                    .menuPrice(new BigDecimal("10000"))
                    .quantity(1)
                    .totalPrice(new BigDecimal("10000"))
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            OrderDetail orderDetail = OrderDetail.builder()
                    .orderId(orderId)
                    .orgMemberId(childOrgMemberId)
                    .shareRatio(1)
                    .build();
            ReflectionTestUtils.setField(orderDetail, "id", 2001L);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.empty());
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(parentGroup, childGroup));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of(childMember));
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of());
            given(storeMenuRepository.findAllByStoreId(any())).willReturn(List.of());
            given(orgMemberRepository.findAllById(List.of(childOrgMemberId)))
                    .willReturn(List.of(childOrgMember));
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of(order));
            given(orderDetailRepository.findByOrderIdIn(List.of(orderId)))
                    .willReturn(List.of(orderDetail));

            // when
            List<GroupResponse> roots = groupService.getGroups(roundId, userId);

            // then
            assertThat(roots).hasSize(1);
            GroupResponse parent = roots.get(0);
            assertThat(parent.getGroupId()).isEqualTo(parentGroupId);
            assertThat(parent.getChildGroups()).hasSize(1);

            // 자식 그룹의 totalAmount = 10,000원
            GroupResponse child = parent.getChildGroups().get(0);
            assertThat(child.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));

            // 부모 totalAmount = 자식 totalAmount(10,000) 포함 = 10,000원
            assertThat(parent.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        /**
         * 부모 그룹 자체 금액과 자식 그룹 금액이 모두 합산되는지 검증한다.
         *
         * <p>구조:
         * <ul>
         *   <li>부모 그룹 (ID=200): 공유 메뉴 5,000원 x 2개 = 10,000원</li>
         *   <li>자식 그룹 (ID=201): orgMember(ID=51)가 8,000원 주문</li>
         * </ul>
         * 기대: 부모 totalAmount = 10,000 + 8,000 = 18,000원</p>
         */
        @Test
        @DisplayName("부모 자체 금액과 자식 금액이 모두 합산된다")
        void getGroups_parentOwnAmountAndChildAmountBothIncluded() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;
            Long parentGroupId = 200L;
            Long childGroupId = 201L;
            Long childOrgMemberId = 51L;
            Long orderId = 1001L;
            Long storeId = 5L;
            Long menuId = 10L;

            Round round = createRound(roundId, sessionId);
            ReflectionTestUtils.setField(round, "storeId", storeId);

            RoundGroup parentGroup = createRoundGroup(parentGroupId, roundId, null, "부모그룹", 0);
            RoundGroup childGroup = createRoundGroup(childGroupId, roundId, parentGroupId, "자식그룹", 1);
            RoundGroupMember childMember = createRoundGroupMember(2L, childGroupId, childOrgMemberId);
            OrgMember childOrgMember = createOrgMember(childOrgMemberId, sessionId, null, "김철수");

            // 부모 그룹의 공유 메뉴: 5,000원 x 2개
            RoundGroupSharedMenu sharedMenu = createRoundGroupSharedMenu(1L, parentGroupId, menuId, 2);
            StoreMenu storeMenu = createStoreMenu(menuId, "치킨", new BigDecimal("5000"));

            // 자식 멤버의 주문: 8,000원 x 1개
            Order order = Order.builder()
                    .roundId(roundId)
                    .groupId(childGroupId)
                    .groupName("자식그룹")
                    .menuId(menuId)
                    .menuName("피자")
                    .menuPrice(new BigDecimal("8000"))
                    .quantity(1)
                    .totalPrice(new BigDecimal("8000"))
                    .build();
            ReflectionTestUtils.setField(order, "id", orderId);

            OrderDetail orderDetail = OrderDetail.builder()
                    .orderId(orderId)
                    .orgMemberId(childOrgMemberId)
                    .shareRatio(1)
                    .build();
            ReflectionTestUtils.setField(orderDetail, "id", 2001L);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
            given(orgMemberRepository.findBySessionIdAndUserId(sessionId, userId))
                    .willReturn(Optional.empty());
            given(roundGroupRepository.findByRoundId(roundId))
                    .willReturn(List.of(parentGroup, childGroup));
            given(roundGroupMemberRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of(childMember));
            given(roundGroupSharedMenuRepository.findByGroupIdIn(List.of(parentGroupId, childGroupId)))
                    .willReturn(List.of(sharedMenu));
            given(storeMenuRepository.findAllByStoreId(storeId)).willReturn(List.of(storeMenu));
            given(orgMemberRepository.findAllById(List.of(childOrgMemberId)))
                    .willReturn(List.of(childOrgMember));
            given(orderRepository.findByRoundId(roundId)).willReturn(List.of(order));
            given(orderDetailRepository.findByOrderIdIn(List.of(orderId)))
                    .willReturn(List.of(orderDetail));

            // when
            List<GroupResponse> roots = groupService.getGroups(roundId, userId);

            // then
            assertThat(roots).hasSize(1);
            GroupResponse parent = roots.get(0);

            // 자식 totalAmount = 8,000원
            GroupResponse child = parent.getChildGroups().get(0);
            assertThat(child.getTotalAmount()).isEqualByComparingTo(new BigDecimal("8000"));

            // 부모 totalAmount = 공유메뉴(10,000) + 자식(8,000) = 18,000원
            assertThat(parent.getTotalAmount()).isEqualByComparingTo(new BigDecimal("18000"));
        }
    }

    @Nested
    @DisplayName("CLOSED 라운드 수정 방지 테스트")
    class ClosedRoundGuardTest {

        @Test
        @DisplayName("CLOSED 라운드에서 그룹 생성 시 예외 발생")
        void createGroup_closedRound_throwsException() {
            // given
            Long roundId = 1L;
            Long userId = 100L;
            Long sessionId = 10L;

            Round closedRound = createRound(roundId, sessionId);
            closedRound.changeStatus(RoundStatus.CLOSED);

            GroupCreateRequest request = new GroupCreateRequest();
            ReflectionTestUtils.setField(request, "name", "1조");
            ReflectionTestUtils.setField(request, "parentGroupId", null);

            given(roundRepository.findById(roundId)).willReturn(Optional.of(closedRound));

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(roundId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ROUND_ALREADY_CLOSED);
        }
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private Round createRound(Long id, Long sessionId) {
        Round round = Round.builder()
                .sessionId(sessionId)
                .title("테스트 라운드")
                .status(RoundStatus.OPEN)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private RoundGroup createRoundGroup(Long id, Long roundId, Long parentGroupId, String name, int depth) {
        RoundGroup group = RoundGroup.builder()
                .roundId(roundId)
                .parentGroupId(parentGroupId)
                .name(name)
                .depth(depth)
                .build();
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private RoundGroupMember createRoundGroupMember(Long id, Long groupId, Long orgMemberId) {
        RoundGroupMember member = RoundGroupMember.builder()
                .groupId(groupId)
                .orgMemberId(orgMemberId)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private RoundGroupSharedMenu createRoundGroupSharedMenu(Long id, Long groupId, Long menuId, int quantity) {
        RoundGroupSharedMenu sharedMenu = RoundGroupSharedMenu.builder()
                .groupId(groupId)
                .menuId(menuId)
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(sharedMenu, "id", id);
        return sharedMenu;
    }

    private OrgMember createOrgMember(Long id, Long sessionId, Long userId, String nickname) {
        OrgMember orgMember = OrgMember.builder()
                .sessionId(sessionId)
                .userId(userId)
                .nickname(nickname)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(orgMember, "id", id);
        return orgMember;
    }

    private Member createMember(Long id, String name) {
        Member member = Member.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name(name)
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private StoreMenu createStoreMenu(Long id, String name, BigDecimal price) {
        StoreMenu storeMenu = StoreMenu.builder()
                .storeId(1L)
                .name(name)
                .price(price)
                .available(true)
                .sortOrder(0)
                .build();
        ReflectionTestUtils.setField(storeMenu, "id", id);
        return storeMenu;
    }
}
