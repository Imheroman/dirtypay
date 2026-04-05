'use client';

import { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import { GroupBubble, MemberBubble } from '../GroupBubble';
import { GroupCreateDialog } from '../GroupCreateDialog';
import { GroupJoinDialog } from '../GroupJoinDialog';
import { SharedMenuDialog } from '../SharedMenuDialog';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  useCreateGroupMutation,
  useJoinGroupWithLinkMutation,
  useLeaveGroupMutation,
  useDeleteGroupMutation,
  useUpdateGroupMutation,
  useSaveSharedMenusMutation,
  useMoveGroupMutation,
} from '../../hooks';
import { findMyGroup } from '../../utils';
import type { RoundGroup, RoundGroupMember, SharedMenu, Menu } from '../../types';
import type { RoundParticipant } from '@/features/round/types';

function findGroupById(groups: RoundGroup[], groupId: number): RoundGroup | null {
  for (const g of groups) {
    if (g.groupId === groupId) return g;
    const found = findGroupById(g.childGroups, groupId);
    if (found) return found;
  }
  return null;
}

function hasMembers(group: RoundGroup): boolean {
  if (group.members.length > 0) return true;
  return group.childGroups.some(hasMembers);
}

interface GroupsTabProps {
  roundId: string;
  groups: RoundGroup[];
  availableMenus: Menu[];
  sessionId: number;
  currentMemberId: number;
  isReadOnly?: boolean;
  unassignedParticipants?: RoundParticipant[];
}

export function GroupsTab({ roundId, groups, availableMenus, sessionId, currentMemberId, isReadOnly, unassignedParticipants }: GroupsTabProps) {
  // 다이얼로그 상태
  const [createGroupOpen, setCreateGroupOpen] = useState(false);
  const [createGroupParentId, setCreateGroupParentId] = useState<number | undefined>();
  const [joinGroupOpen, setJoinGroupOpen] = useState(false);
  const [selectedGroupToJoin, setSelectedGroupToJoin] = useState<RoundGroup | null>(null);
  const [sharedMenuDialogOpen, setSharedMenuDialogOpen] = useState(false);
  const [selectedGroupForSharedMenu, setSelectedGroupForSharedMenu] = useState<RoundGroup | null>(null);
  const [leaveConfirmGroupId, setLeaveConfirmGroupId] = useState<number | null>(null);
  const [deleteConfirmGroup, setDeleteConfirmGroup] = useState<RoundGroup | null>(null);
  const [renameGroupId, setRenameGroupId] = useState<number | null>(null);
  const [renameGroupName, setRenameGroupName] = useState('');

  // DnD 상태
  const [activeDragData, setActiveDragData] = useState<{
    member: RoundGroupMember;
    groupId: number;
  } | null>(null);

  // Sensors
  const pointerSensor = useSensor(PointerSensor, {
    activationConstraint: { distance: 8 },
  });
  const touchSensor = useSensor(TouchSensor, {
    activationConstraint: { delay: 200, tolerance: 5 },
  });
  const sensors = useSensors(pointerSensor, touchSensor);

  // Mutations
  const createGroupMutation = useCreateGroupMutation();
  const joinGroupWithLinkMutation = useJoinGroupWithLinkMutation();
  const leaveGroupMutation = useLeaveGroupMutation();
  const saveSharedMenusMutation = useSaveSharedMenusMutation();
  const moveGroupMutation = useMoveGroupMutation();
  const deleteGroupMutation = useDeleteGroupMutation();
  const updateGroupMutation = useUpdateGroupMutation();

  const handleCreateGroup = (parentGroupId?: number) => {
    setCreateGroupParentId(parentGroupId);
    setCreateGroupOpen(true);
  };

  const handleCreateGroupSubmit = (data: { name: string; parentGroupId?: number }) => {
    const currentGroup = findMyGroup(groups, currentMemberId);
    createGroupMutation.mutate({
      roundId: Number(roundId),
      request: { name: data.name, parentGroupId: data.parentGroupId },
      currentGroupId: currentGroup?.groupId,
    });
  };

  const handleJoinGroup = (groupId: number) => {
    const findGroup = (items: RoundGroup[]): RoundGroup | null => {
      for (const g of items) {
        if (g.groupId === groupId) return g;
        const found = findGroup(g.childGroups);
        if (found) return found;
      }
      return null;
    };
    const group = findGroup(groups);
    if (group) {
      setSelectedGroupToJoin(group);
      setJoinGroupOpen(true);
    }
  };

  const handleJoinGroupConfirm = (groupId: number, memberId: number) => {
    const currentGroup = findMyGroup(groups, currentMemberId);
    joinGroupWithLinkMutation.mutate({
      groupId,
      memberId,
      sessionId,
      roundId: Number(roundId),
      currentGroupId: currentGroup?.groupId,
    });
    setSelectedGroupToJoin(null);
  };

  const handleLeaveGroup = (groupId: number) => {
    setLeaveConfirmGroupId(groupId);
  };

  const handleLeaveGroupConfirm = () => {
    if (leaveConfirmGroupId === null) return;
    leaveGroupMutation.mutate(
      { groupId: leaveConfirmGroupId, roundId: Number(roundId) },
      { onSettled: () => setLeaveConfirmGroupId(null) },
    );
  };

  const handleDeleteGroup = (groupId: number) => {
    const group = findGroupById(groups, groupId);
    if (!group) return;
    setDeleteConfirmGroup(group);
  };

  const handleDeleteGroupConfirm = () => {
    if (!deleteConfirmGroup) return;
    deleteGroupMutation.mutate(
      { groupId: deleteConfirmGroup.groupId, roundId: Number(roundId) },
      { onSettled: () => setDeleteConfirmGroup(null) },
    );
  };

  const handleRenameGroup = (groupId: number, currentName: string) => {
    setRenameGroupId(groupId);
    setRenameGroupName(currentName);
  };

  const handleRenameGroupSubmit = () => {
    if (renameGroupId === null || !renameGroupName.trim()) return;
    updateGroupMutation.mutate(
      { groupId: renameGroupId, roundId: Number(roundId), request: { name: renameGroupName.trim() } },
      { onSettled: () => { setRenameGroupId(null); setRenameGroupName(''); } },
    );
  };

  const handleAddSharedMenu = (groupId: number) => {
    const findGroup = (items: RoundGroup[]): RoundGroup | null => {
      for (const g of items) {
        if (g.groupId === groupId) return g;
        const found = findGroup(g.childGroups);
        if (found) return found;
      }
      return null;
    };
    const group = findGroup(groups);
    if (group) {
      setSelectedGroupForSharedMenu(group);
      setSharedMenuDialogOpen(true);
    }
  };

  const handleSaveSharedMenus = (menus: SharedMenu[]) => {
    if (!selectedGroupForSharedMenu) return;

    saveSharedMenusMutation.mutate({
      groupId: selectedGroupForSharedMenu.groupId,
      roundId: Number(roundId),
      request: {
        menus: menus.map((m) => ({ menuId: m.menuId, quantity: m.quantity })),
      },
    });

    setSelectedGroupForSharedMenu(null);
  };

  // DnD 핸들러
  const handleDragStart = (event: DragStartEvent) => {
    const { member, sourceGroupId } = event.active.data.current as {
      member: RoundGroupMember;
      sourceGroupId: number;
    };
    setActiveDragData({ member, groupId: sourceGroupId });
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveDragData(null);

    const { active, over } = event;
    if (!over) return;

    const sourceGroupId = (active.data.current as { sourceGroupId: number }).sourceGroupId;
    const targetGroupId = (over.data.current as { groupId: number }).groupId;

    if (sourceGroupId === targetGroupId) return;

    moveGroupMutation.mutate({
      sourceGroupId,
      targetGroupId,
      memberId: currentMemberId,
      sessionId,
      roundId: Number(roundId),
    });
  };

  const handleMoveToGroup = (sourceGroupId: number, targetGroupId: number) => {
    moveGroupMutation.mutate({
      sourceGroupId,
      targetGroupId,
      memberId: currentMemberId,
      sessionId,
      roundId: Number(roundId),
    });
  };

  return (
    <>
      <DndContext
        sensors={isReadOnly ? [] : sensors}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <GroupBubble
          groups={groups}
          currentMemberId={currentMemberId}
          onJoinGroup={isReadOnly ? undefined : handleJoinGroup}
          onLeaveGroup={isReadOnly ? undefined : handleLeaveGroup}
          onCreateGroup={isReadOnly ? undefined : handleCreateGroup}
          onMoveToGroup={isReadOnly ? undefined : handleMoveToGroup}
          onDeleteGroup={isReadOnly ? undefined : handleDeleteGroup}
          onRenameGroup={isReadOnly ? undefined : handleRenameGroup}
          unassignedParticipants={unassignedParticipants}
        />

        <DragOverlay>
          {activeDragData && (
            <MemberBubble
              member={activeDragData.member}
              groupId={activeDragData.groupId}
              currentMemberId={currentMemberId}
            />
          )}
        </DragOverlay>
      </DndContext>

      <GroupCreateDialog
        open={createGroupOpen}
        onOpenChange={setCreateGroupOpen}
        parentGroups={groups}
        defaultParentId={createGroupParentId}
        onSubmit={handleCreateGroupSubmit}
      />

      <GroupJoinDialog
        open={joinGroupOpen}
        onOpenChange={setJoinGroupOpen}
        group={selectedGroupToJoin}
        currentMemberId={currentMemberId}
        onConfirm={handleJoinGroupConfirm}
        currentGroupName={findMyGroup(groups, currentMemberId)?.groupName}
      />

      <SharedMenuDialog
        open={sharedMenuDialogOpen}
        onOpenChange={setSharedMenuDialogOpen}
        groupName={selectedGroupForSharedMenu?.groupName ?? ''}
        currentSharedMenus={selectedGroupForSharedMenu?.sharedMenus ?? []}
        availableMenus={availableMenus}
        onSave={handleSaveSharedMenus}
      />

      <ConfirmModal
        isOpen={leaveConfirmGroupId !== null}
        onClose={() => setLeaveConfirmGroupId(null)}
        onConfirm={handleLeaveGroupConfirm}
        title="정말 그룹에서 나갈까요?"
        description="나가면 이 그룹의 공유 메뉴 비용이 재계산돼요."
        confirmText="나가기"
        variant="destructive"
        isLoading={leaveGroupMutation.isPending}
      />

      {deleteConfirmGroup && hasMembers(deleteConfirmGroup) ? (
        <ConfirmModal
          isOpen={deleteConfirmGroup !== null}
          onClose={() => setDeleteConfirmGroup(null)}
          onConfirm={() => setDeleteConfirmGroup(null)}
          title="그룹을 삭제할 수 없어요"
          description="참여 중인 인원이 있어요. 모든 인원이 나간 후에 삭제할 수 있어요."
          confirmText="확인"
          showCancel={false}
        />
      ) : (
        <ConfirmModal
          isOpen={deleteConfirmGroup !== null}
          onClose={() => setDeleteConfirmGroup(null)}
          onConfirm={handleDeleteGroupConfirm}
          title="정말 삭제할까요?"
          description={
            deleteConfirmGroup && deleteConfirmGroup.childGroups.length > 0
              ? '하위 그룹도 함께 삭제돼요. 삭제하면 되돌릴 수 없어요.'
              : '삭제하면 되돌릴 수 없어요.'
          }
          confirmText="삭제"
          variant="destructive"
          isLoading={deleteGroupMutation.isPending}
        />
      )}

      <Dialog
        open={renameGroupId !== null}
        onOpenChange={(open) => {
          if (!open) { setRenameGroupId(null); setRenameGroupName(''); }
        }}
      >
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>그룹 이름 변경</DialogTitle>
            <DialogDescription>새로운 그룹 이름을 입력해 주세요.</DialogDescription>
          </DialogHeader>
          <Input
            autoFocus
            value={renameGroupName}
            onChange={(e) => setRenameGroupName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleRenameGroupSubmit(); }}
            placeholder="그룹 이름"
          />
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => { setRenameGroupId(null); setRenameGroupName(''); }}
            >
              취소
            </Button>
            <Button
              onClick={handleRenameGroupSubmit}
              disabled={!renameGroupName.trim() || updateGroupMutation.isPending}
            >
              {updateGroupMutation.isPending ? '변경 중...' : '변경'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
