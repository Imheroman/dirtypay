'use client';

import { useState, memo, useCallback } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  ChevronRightIcon,
  ChevronDownIcon,
  FolderIcon,
  UserIcon,
} from '@/components/common/Icons';
import type { NodeTree } from '../types';
import { countAllMembers } from './TreeNode';

interface MemberSelectTreeProps {
  nodes: NodeTree[];
  selectedIds: Set<number>;
  onToggleMember: (memberId: number) => void;
  onToggleNode: (node: NodeTree) => void;
  onSelectAll: () => void;
  onClearAll: () => void;
  isNodeFullySelected: (node: NodeTree) => boolean;
  isNodePartiallySelected: (node: NodeTree) => boolean;
}

export function MemberSelectTree({
  nodes,
  selectedIds,
  onToggleMember,
  onToggleNode,
  onSelectAll,
  onClearAll,
  isNodeFullySelected,
  isNodePartiallySelected,
}: MemberSelectTreeProps) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-sm text-muted-foreground">
          {selectedIds.size}명 선택됨
        </span>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={onSelectAll}>
            전체 선택
          </Button>
          <Button variant="outline" size="sm" onClick={onClearAll}>
            전체 해제
          </Button>
        </div>
      </div>

      <div className="rounded-md border">
        {nodes.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">
            조직도가 없어요
          </p>
        ) : (
          nodes.map((node) => (
            <MemberSelectNode
              key={node.id}
              node={node}
              depth={0}
              selectedIds={selectedIds}
              onToggleMember={onToggleMember}
              onToggleNode={onToggleNode}
              isNodeFullySelected={isNodeFullySelected}
              isNodePartiallySelected={isNodePartiallySelected}
            />
          ))
        )}
      </div>
    </div>
  );
}

interface MemberSelectNodeProps {
  node: NodeTree;
  depth: number;
  selectedIds: Set<number>;
  onToggleMember: (memberId: number) => void;
  onToggleNode: (node: NodeTree) => void;
  isNodeFullySelected: (node: NodeTree) => boolean;
  isNodePartiallySelected: (node: NodeTree) => boolean;
}

const MemberSelectNode = memo(function MemberSelectNode({
  node,
  depth,
  selectedIds,
  onToggleMember,
  onToggleNode,
  isNodeFullySelected,
  isNodePartiallySelected,
}: MemberSelectNodeProps) {
  const [expanded, setExpanded] = useState(depth === 0);
  const memberCount = countAllMembers(node);
  const hasContent = node.children.length > 0 || node.members.length > 0;

  const fullySelected = isNodeFullySelected(node);
  const partiallySelected = isNodePartiallySelected(node);

  const handleToggleNode = useCallback(() => {
    onToggleNode(node);
  }, [onToggleNode, node]);

  const checkboxState = fullySelected
    ? true
    : partiallySelected
      ? 'indeterminate'
      : false;

  return (
    <div>
      {/* Node header with checkbox */}
      <div
        className="flex items-center gap-2 px-2 py-1.5 hover:bg-accent transition-colors"
        style={{ paddingLeft: `${depth * 1.5 + 0.5}rem` }}
      >
        {hasContent && (
          <Checkbox
            checked={checkboxState}
            onCheckedChange={handleToggleNode}
            aria-label={`${node.name} 전체 선택`}
          />
        )}

        <button
          onClick={() => setExpanded(!expanded)}
          className="flex flex-1 items-center gap-2 text-left"
          aria-expanded={expanded}
          aria-label={`${node.name} 하위 목록 ${expanded ? '접기' : '펼치기'}`}
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
          <FolderIcon className="h-4 w-4 shrink-0 text-primary" />
          <span className="text-sm font-medium text-foreground truncate">
            {node.name}
          </span>
          <Badge variant="outline" className="ml-auto text-xs shrink-0">
            {memberCount}명
          </Badge>
        </button>
      </div>

      {/* Expanded content */}
      {expanded && (
        <div>
          {/* Members */}
          {node.members.map((member) => (
            <label
              key={member.id}
              className="flex items-center gap-2 px-2 py-1.5 hover:bg-accent/50 transition-colors cursor-pointer"
              style={{ paddingLeft: `${(depth + 1) * 1.5 + 1}rem` }}
            >
              <Checkbox
                checked={selectedIds.has(member.id)}
                onCheckedChange={() => onToggleMember(member.id)}
                aria-label={`${member.nickname} 선택`}
              />
              <UserIcon className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
              <span className="text-sm text-foreground truncate">
                {member.nickname}
              </span>
              {!member.isActive && (
                <Badge variant="secondary" className="text-xs">
                  비활성
                </Badge>
              )}
            </label>
          ))}

          {/* Child nodes */}
          {node.children.map((child) => (
            <MemberSelectNode
              key={child.id}
              node={child}
              depth={depth + 1}
              selectedIds={selectedIds}
              onToggleMember={onToggleMember}
              onToggleNode={onToggleNode}
              isNodeFullySelected={isNodeFullySelected}
              isNodePartiallySelected={isNodePartiallySelected}
            />
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
