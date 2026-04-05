'use client';

import { useState } from 'react';
import { useDraggable, useDroppable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  UserIcon,
  UsersIcon,
  XIcon,
  PlusIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  TrashIcon,
  MoreVerticalIcon,
  EditIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { RoundGroup, RoundGroupMember } from '../types';
import type { RoundParticipant } from '@/features/round/types';

// 깊이에 따른 참여 중 그룹 색상
const participatingDepthColors = [
  'bg-primary/10 border-primary/30',
  'bg-primary/15 border-primary/40',
  'bg-primary/20 border-primary/50',
  'bg-primary/25 border-primary/60',
];

// 깊이에 따른 미참여 그룹 색상
const notParticipatingDepthColors = [
  'bg-muted/30 border-muted-foreground/20',
  'bg-muted/40 border-muted-foreground/25',
  'bg-muted/50 border-muted-foreground/30',
  'bg-muted/60 border-muted-foreground/35',
];

export interface GroupBubbleProps {
  groups: RoundGroup[];
  currentMemberId: number;
  onJoinGroup?: (groupId: number) => void;
  onLeaveGroup?: (groupId: number) => void;
  onCreateGroup?: (parentGroupId?: number) => void;
  onMoveToGroup?: (sourceGroupId: number, targetGroupId: number) => void;
  onDeleteGroup?: (groupId: number) => void;
  onRenameGroup?: (groupId: number, currentName: string) => void;
  unassignedParticipants?: RoundParticipant[];
}

export function GroupBubble({
  groups,
  currentMemberId,
  onJoinGroup,
  onLeaveGroup,
  onCreateGroup,
  onMoveToGroup,
  onDeleteGroup,
  onRenameGroup,
  unassignedParticipants,
}: GroupBubbleProps) {
  const totalAmount = groups.reduce((sum, g) => sum + g.totalAmount, 0);

  return (
    <div className="space-y-4">
      {/* 전체 요약 */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <UsersIcon className="w-5 h-5 text-primary" />
          <span className="font-semibold text-foreground">그룹별 주문 현황</span>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted-foreground">
            총 {formatAmount(totalAmount)}원
          </span>
          {onCreateGroup && (
            <Button
              variant="outline"
              size="sm"
              className="h-8 bg-transparent"
              onClick={() => onCreateGroup()}
            >
              <PlusIcon className="w-4 h-4 mr-1" />
              그룹 만들기
            </Button>
          )}
        </div>
      </div>

      {/* 그룹 버블들 */}
      <div className="space-y-3">
        {groups.map((group) => (
          <GroupBubbleItem
            key={group.groupId}
            group={group}
            depth={0}
            currentMemberId={currentMemberId}
            onJoinGroup={onJoinGroup}
            onLeaveGroup={onLeaveGroup}
            onCreateGroup={onCreateGroup}
            onMoveToGroup={onMoveToGroup}
            onDeleteGroup={onDeleteGroup}
            onRenameGroup={onRenameGroup}
          />
        ))}
      </div>

      {groups.length === 0 && (
        <div className="text-center py-8 text-muted-foreground">
          <UsersIcon className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>아직 그룹이 없어요</p>
          {onCreateGroup && (
            <Button
              variant="outline"
              className="mt-4 bg-transparent"
              onClick={() => onCreateGroup()}
            >
              <PlusIcon className="w-4 h-4 mr-2" />
              첫 그룹 만들기
            </Button>
          )}
        </div>
      )}

      {/* 미배정 멤버 섹션 */}
      {unassignedParticipants && unassignedParticipants.length > 0 && (
        <div className="mt-4 rounded-2xl border-2 border-dashed border-muted-foreground/20 bg-muted/30 p-4">
          <div className="flex items-center gap-2 mb-3">
            <UserIcon className="w-4 h-4 text-muted-foreground" />
            <span className="text-sm font-medium text-muted-foreground">
              미배정
            </span>
            <Badge variant="outline" className="text-xs">
              {unassignedParticipants.length}명
            </Badge>
          </div>
          <div className="flex flex-wrap gap-2">
            {unassignedParticipants.map((p) => (
              <div
                key={p.id}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-background border border-border shadow-sm"
              >
                <div className="w-5 h-5 rounded-full bg-secondary flex items-center justify-center">
                  <UserIcon className="w-3 h-3 text-secondary-foreground" />
                </div>
                <span className="text-sm font-medium text-foreground">
                  {p.nickname}
                </span>
                {p.isExcluded && (
                  <Badge variant="secondary" className="text-xs">
                    제외
                  </Badge>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

interface GroupBubbleItemProps {
  group: RoundGroup;
  depth: number;
  currentMemberId: number;
  onJoinGroup?: (groupId: number) => void;
  onLeaveGroup?: (groupId: number) => void;
  onCreateGroup?: (parentGroupId?: number) => void;
  onMoveToGroup?: (sourceGroupId: number, targetGroupId: number) => void;
  onDeleteGroup?: (groupId: number) => void;
  onRenameGroup?: (groupId: number, currentName: string) => void;
}

function GroupBubbleItem({
  group,
  depth,
  currentMemberId,
  onJoinGroup,
  onLeaveGroup,
  onCreateGroup,
  onMoveToGroup,
  onDeleteGroup,
  onRenameGroup,
}: GroupBubbleItemProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  const { isOver, setNodeRef: setDroppableRef } = useDroppable({
    id: `group-${group.groupId}`,
    data: { groupId: group.groupId },
  });

  const colorClass = group.isParticipating
    ? participatingDepthColors[Math.min(depth, participatingDepthColors.length - 1)]
    : notParticipatingDepthColors[Math.min(depth, notParticipatingDepthColors.length - 1)];

  const memberCount = group.members.length;
  const hasCurrentUser = group.members.some((m) => m.isCurrentUser || m.orgMemberId === currentMemberId);

  const handleGroupClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    if (target.closest('button, [role="button"], a')) return;
    e.stopPropagation();
    if (hasCurrentUser) {
      onLeaveGroup?.(group.groupId);
    } else {
      onJoinGroup?.(group.groupId);
    }
  };

  return (
    <div
      ref={setDroppableRef}
      onClick={handleGroupClick}
      className={cn(
        'relative rounded-2xl border-2 p-4 transition-all',
        group.isParticipating ? 'border-solid' : 'border-dashed',
        colorClass,
        isOver && !hasCurrentUser && 'ring-2 ring-primary ring-offset-2',
        hasCurrentUser && 'shadow-sm ring-1 ring-primary/20 hover:shadow-md hover:ring-primary/30',
        hasCurrentUser && onLeaveGroup && 'cursor-pointer hover:border-destructive/30 active:scale-[0.99]',
        !hasCurrentUser && onJoinGroup && 'cursor-pointer hover:bg-muted/50 hover:border-muted-foreground/40 hover:shadow-md active:scale-[0.99]',
      )}
    >
      {/* 그룹 헤더 */}
      <div className="group/header flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-2.5 -ml-2 rounded hover:bg-background/50 transition-colors"
            aria-expanded={isExpanded}
            aria-label={`${group.groupName} ${isExpanded ? '접기' : '펼치기'}`}
          >
            {isExpanded ? (
              <ChevronDownIcon className="w-4 h-4 text-muted-foreground" />
            ) : (
              <ChevronUpIcon className="w-4 h-4 text-muted-foreground" />
            )}
          </button>
          <span className="font-semibold text-foreground">{group.groupName}</span>
          <Badge
            variant={group.isParticipating ? 'default' : 'outline'}
            className="text-xs"
          >
            {memberCount}명
          </Badge>
          {hasCurrentUser && (
            <Badge variant="secondary" className="text-xs">
              참여 중
            </Badge>
          )}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-foreground">
            {formatAmount(group.totalAmount)}원
          </span>
          {(onDeleteGroup || onRenameGroup) && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  type="button"
                  aria-label="그룹 메뉴"
                  className="p-1.5 rounded-lg opacity-0 group-hover/header:opacity-100 transition-opacity text-muted-foreground hover:bg-accent"
                  onClick={(e) => e.stopPropagation()}
                >
                  <MoreVerticalIcon className="w-4 h-4" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
                {onRenameGroup && (
                  <DropdownMenuItem onClick={() => onRenameGroup(group.groupId, group.groupName)}>
                    <EditIcon className="w-4 h-4" />
                    이름 변경
                  </DropdownMenuItem>
                )}
                {onRenameGroup && onDeleteGroup && <DropdownMenuSeparator />}
                {onDeleteGroup && (
                  <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    onClick={() => onDeleteGroup(group.groupId)}
                  >
                    <TrashIcon className="w-4 h-4" />
                    삭제
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </div>

      {isExpanded && (
        <>
          {/* 공유 메뉴 */}
          {group.sharedMenus.length > 0 && (
            <div className="mb-3">
              <p className="text-xs text-muted-foreground mb-2">공유 메뉴</p>
              <div className="flex flex-wrap gap-2">
                {group.sharedMenus.map((menu) => (
                  <Badge
                    key={menu.menuId}
                    variant="secondary"
                    className="text-xs font-normal"
                  >
                    {menu.menuName} x{menu.quantity}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {/* 멤버 목록 */}
          <div className="flex flex-wrap gap-2 mb-3">
            {group.members.map((member) => (
              <MemberBubble
                key={member.orgMemberId}
                member={member}
                groupId={group.groupId}
                currentMemberId={currentMemberId}
                onLeaveGroup={onLeaveGroup}
              />
            ))}
          </div>

          {/* 하위 그룹 */}
          {group.childGroups.length > 0 && (
            <div className="relative z-20 space-y-3 mt-4">
              {group.childGroups.map((childGroup) => (
                <GroupBubbleItem
                  key={childGroup.groupId}
                  group={childGroup}
                  depth={depth + 1}
                  currentMemberId={currentMemberId}
                  onJoinGroup={onJoinGroup}
                  onLeaveGroup={onLeaveGroup}
                  onCreateGroup={onCreateGroup}
                  onMoveToGroup={onMoveToGroup}
                  onDeleteGroup={onDeleteGroup}
                  onRenameGroup={onRenameGroup}
                />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

interface MemberBubbleProps {
  member: RoundGroupMember;
  groupId: number;
  currentMemberId?: number;
  onLeaveGroup?: (groupId: number) => void;
}

export function MemberBubble({ member, groupId, currentMemberId, onLeaveGroup }: MemberBubbleProps) {
  const isMe = member.isCurrentUser || member.orgMemberId === currentMemberId;

  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `member-${member.orgMemberId}-group-${groupId}`,
    data: { memberId: member.orgMemberId, sourceGroupId: groupId, member },
    disabled: !isMe,
  });

  const style = transform
    ? { transform: CSS.Translate.toString(transform) }
    : undefined;

  const orderCount = member.personalOrders.length;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className={cn(
        'group relative inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full shadow-sm hover:shadow-md transition-shadow',
        isMe
          ? 'bg-primary text-primary-foreground cursor-grab active:cursor-grabbing'
          : 'bg-background border border-border',
        isDragging && 'opacity-40'
      )}
    >
      <div
        className={cn(
          'w-5 h-5 rounded-full flex items-center justify-center',
          isMe ? 'bg-primary-foreground/20' : 'bg-secondary'
        )}
      >
        <UserIcon
          className={cn(
            'w-3 h-3',
            isMe ? 'text-primary-foreground' : 'text-secondary-foreground'
          )}
        />
      </div>
      <span
        className={cn(
          'text-sm font-medium',
          isMe ? 'text-primary-foreground' : 'text-foreground'
        )}
      >
        {member.nickname}
        {isMe && ' (나)'}
      </span>
      {orderCount > 0 && (
        <Badge
          variant={isMe ? 'secondary' : 'outline'}
          className="text-xs h-5 px-1.5"
        >
          {orderCount}
        </Badge>
      )}
      {isMe && onLeaveGroup && (
        <button
          type="button"
          aria-label="그룹 나가기"
          className="ml-0.5 p-0.5 rounded-full opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity hover:bg-primary-foreground/20"
          onPointerDown={(e) => e.stopPropagation()}
          onClick={(e) => {
            e.stopPropagation();
            onLeaveGroup(groupId);
          }}
        >
          <XIcon className="w-3.5 h-3.5 text-primary-foreground" />
        </button>
      )}
    </div>
  );
}