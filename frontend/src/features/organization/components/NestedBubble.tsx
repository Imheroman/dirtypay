'use client';

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
  PlusIcon,
  UsersIcon,
  UserIcon,
  MoreVerticalIcon,
  UserPlusIcon,
} from '@/components/common/Icons';
import type { NodeTree, Member } from '../types';
import { countAllMembers } from './TreeNode';

// 깊이에 따른 색상
const depthColors = [
  'bg-primary/5 border-primary/20',
  'bg-primary/10 border-primary/30',
  'bg-primary/15 border-primary/40',
  'bg-primary/20 border-primary/50',
];

interface MemberPillProps {
  member: Member;
  onEdit: (member: Member) => void;
  onDelete: (member: Member) => void;
  onToggleActive: (member: Member) => void;
}

function MemberPill({ member, onEdit, onDelete, onToggleActive }: MemberPillProps) {
  return (
    <div
      className={`group relative inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full shadow-sm hover:shadow-md transition-shadow bg-background border border-border ${
        !member.isActive ? 'opacity-50' : ''
      }`}
    >
      <div className="w-5 h-5 rounded-full flex items-center justify-center bg-secondary">
        <UserIcon className="w-3 h-3 text-secondary-foreground" />
      </div>
      <span className="text-sm font-medium text-foreground">
        {member.nickname}
      </span>
      {!member.isActive && (
        <Badge variant="secondary" className="text-[10px] px-1">
          비활성
        </Badge>
      )}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="opacity-0 group-hover:opacity-100 ml-1 p-0.5 rounded transition-all hover:bg-accent">
            <MoreVerticalIcon className="w-3 h-3 text-muted-foreground" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => onEdit(member)}>
            이름 수정
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => onToggleActive(member)}>
            {member.isActive ? '비활성으로 변경' : '활성으로 변경'}
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            className="text-destructive"
            onClick={() => onDelete(member)}
          >
            삭제
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

interface NestedBubbleProps {
  node: NodeTree;
  depth?: number;
  onAddMember: (nodeId: number) => void;
  onAddGroup: (parentNodeId: number) => void;
  onEdit: (node: NodeTree | Member) => void;
  onDelete: (node: NodeTree | Member) => void;
  onInvite: (node: NodeTree) => void;
  onToggleActive: (member: Member) => void;
}

export function NestedBubble({
  node,
  depth = 0,
  onAddMember,
  onAddGroup,
  onEdit,
  onDelete,
  onInvite,
  onToggleActive,
}: NestedBubbleProps) {
  const memberCount = countAllMembers(node);
  const colorClass = depthColors[Math.min(depth, depthColors.length - 1)];
  const isRoot = depth === 0;

  return (
    <div
      className={`relative rounded-3xl border-2 border-dashed p-4 ${colorClass} transition-all`}
    >
      {/* 그룹 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span
            className={`font-semibold ${isRoot ? 'text-lg' : 'text-base'} text-foreground`}
          >
            {node.name}
          </span>
          <Badge variant="outline" className="text-xs bg-background">
            {memberCount}명
          </Badge>
        </div>
        <div className="flex items-center gap-1">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-7 w-7 p-0">
                <MoreVerticalIcon className="w-4 h-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onAddMember(node.id)}>
                <UserIcon className="w-4 h-4 mr-2" />
                멤버 추가
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onAddGroup(node.id)}>
                <UsersIcon className="w-4 h-4 mr-2" />
                하위 그룹 추가
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => onInvite(node)}>
                <UserPlusIcon className="w-4 h-4 mr-2" />
                초대하기
              </DropdownMenuItem>
              {!isRoot && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={() => onEdit(node)}>
                    이름 수정
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    className="text-destructive"
                    onClick={() => onDelete(node)}
                  >
                    삭제
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* 멤버 + 자식 노드 */}
      <div className="flex flex-wrap gap-3">
        {/* 멤버 필 */}
        {node.members.map((member) => (
          <MemberPill
            key={member.id}
            member={member}
            onEdit={onEdit}
            onDelete={onDelete}
            onToggleActive={onToggleActive}
          />
        ))}

        {/* 자식 노드 */}
        {node.children.map((child) => (
          <NestedBubble
            key={child.id}
            node={child}
            depth={depth + 1}
            onAddMember={onAddMember}
            onAddGroup={onAddGroup}
            onEdit={onEdit}
            onDelete={onDelete}
            onInvite={onInvite}
            onToggleActive={onToggleActive}
          />
        ))}

        {/* 빠른 추가 버튼 */}
        <button
          onClick={() => onAddMember(node.id)}
          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full border-2 border-dashed border-muted-foreground/30 text-muted-foreground hover:border-primary hover:text-primary transition-colors"
        >
          <PlusIcon className="w-3 h-3" />
          <span className="text-xs">추가</span>
        </button>
      </div>
    </div>
  );
}
