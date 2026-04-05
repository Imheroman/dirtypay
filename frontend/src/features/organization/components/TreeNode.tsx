'use client';

import { useState, memo } from 'react';
import { Badge } from '@/components/ui/badge';
import {
  ChevronRightIcon,
  ChevronDownIcon,
  FolderIcon,
  UserIcon,
} from '@/components/common/Icons';
import { isUnassignedNode } from '../types';
import type { NodeTree, Member } from '../types';

const depthColors = [
  'text-primary',
  'text-primary/80',
  'text-primary/60',
  'text-primary/40',
  'text-primary/20',
];

export function countAllMembers(node: NodeTree): number {
  return (
    node.members.length +
    node.children.reduce((sum, child) => sum + countAllMembers(child), 0)
  );
}

interface TreeNodeProps {
  node: NodeTree;
  depth?: number;
  defaultExpanded?: boolean;
  onMoveMember?: (member: Member) => void;
}

export const TreeNode = memo(function TreeNode({
  node,
  depth = 0,
  defaultExpanded,
  onMoveMember,
}: TreeNodeProps) {
  const [expanded, setExpanded] = useState(defaultExpanded ?? depth === 0);
  const memberCount = countAllMembers(node);
  const hasContent = node.children.length > 0 || node.members.length > 0;
  const showMoveButton = isUnassignedNode(node) && !!onMoveMember;

  return (
    <div>
      {/* Node header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left hover:bg-accent transition-colors"
        style={{ paddingLeft: `${depth * 1.5 + 0.5}rem` }}
      >
        {hasContent ? (
          expanded ? (
            <ChevronDownIcon className="h-4 w-4 shrink-0 text-muted-foreground" />
          ) : (
            <ChevronRightIcon className="h-4 w-4 shrink-0 text-muted-foreground" />
          )
        ) : (
          <span className="w-4 shrink-0" />
        )}
        <FolderIcon
          className={`h-4 w-4 shrink-0 ${depthColors[Math.min(depth, depthColors.length - 1)]}`}
        />
        <span className="text-sm font-medium text-foreground truncate">
          {node.name}
        </span>
        <Badge variant="outline" className="ml-auto text-xs shrink-0">
          {memberCount}명
        </Badge>
      </button>

      {/* Expanded content */}
      {expanded && (
        <div>
          {/* Members */}
          {node.members.map((member) => (
            <div
              key={member.id}
              className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-accent/50 transition-colors"
              style={{ paddingLeft: `${(depth + 1) * 1.5 + 1}rem` }}
            >
              <UserIcon className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
              <span className="text-sm text-foreground truncate">
                {member.nickname}
              </span>
              {!member.isActive && (
                <Badge variant="secondary" className="text-xs">
                  비활성
                </Badge>
              )}
              {showMoveButton && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onMoveMember(member);
                  }}
                  className="ml-auto shrink-0 rounded px-1.5 py-0.5 text-xs text-primary hover:bg-primary/10 transition-colors"
                  title="그룹 배치"
                >
                  배치
                </button>
              )}
            </div>
          ))}

          {/* Child nodes */}
          {node.children.map((child) => (
            <TreeNode key={child.id} node={child} depth={depth + 1} onMoveMember={onMoveMember} />
          ))}

          {/* Empty node */}
          {node.children.length === 0 && node.members.length === 0 && (
            <p
              className="text-xs text-muted-foreground py-1.5"
              style={{ paddingLeft: `${(depth + 1) * 1.5 + 1}rem` }}
            >
              소속된 멤버가 없어요
            </p>
          )}
        </div>
      )}
    </div>
  );
});
