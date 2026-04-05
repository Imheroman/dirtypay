import { useState, useCallback, useMemo } from 'react';
import type { NodeTree } from '../types';

function getAllMemberIds(node: NodeTree): number[] {
  const memberIds = node.members.map((m) => m.id);
  const childMemberIds = node.children.flatMap(getAllMemberIds);
  return [...memberIds, ...childMemberIds];
}

function getAllMemberIdsFromNodes(nodes: NodeTree[]): number[] {
  return nodes.flatMap(getAllMemberIds);
}

export interface UseTreeSelectionReturn {
  selectedIds: Set<number>;
  toggleMember: (memberId: number) => void;
  toggleNode: (node: NodeTree) => void;
  selectAll: () => void;
  clearAll: () => void;
  isSelected: (memberId: number) => boolean;
  isNodeFullySelected: (node: NodeTree) => boolean;
  isNodePartiallySelected: (node: NodeTree) => boolean;
}

export function useTreeSelection(
  nodes: NodeTree[]
): UseTreeSelectionReturn {
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const allMemberIds = useMemo(() => getAllMemberIdsFromNodes(nodes), [nodes]);

  const toggleMember = useCallback((memberId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(memberId)) {
        next.delete(memberId);
      } else {
        next.add(memberId);
      }
      return next;
    });
  }, []);

  const toggleNode = useCallback((node: NodeTree) => {
    const nodeMembers = getAllMemberIds(node);
    if (nodeMembers.length === 0) return;

    setSelectedIds((prev) => {
      const allSelected = nodeMembers.every((id) => prev.has(id));
      const next = new Set(prev);

      if (allSelected) {
        nodeMembers.forEach((id) => next.delete(id));
      } else {
        nodeMembers.forEach((id) => next.add(id));
      }
      return next;
    });
  }, []);

  const selectAll = useCallback(() => {
    setSelectedIds(new Set(allMemberIds));
  }, [allMemberIds]);

  const clearAll = useCallback(() => {
    setSelectedIds(new Set());
  }, []);

  const isSelected = useCallback(
    (memberId: number) => selectedIds.has(memberId),
    [selectedIds]
  );

  const isNodeFullySelected = useCallback(
    (node: NodeTree) => {
      const nodeMembers = getAllMemberIds(node);
      if (nodeMembers.length === 0) return false;
      return nodeMembers.every((id) => selectedIds.has(id));
    },
    [selectedIds]
  );

  const isNodePartiallySelected = useCallback(
    (node: NodeTree) => {
      const nodeMembers = getAllMemberIds(node);
      if (nodeMembers.length === 0) return false;
      const selectedCount = nodeMembers.filter((id) =>
        selectedIds.has(id)
      ).length;
      return selectedCount > 0 && selectedCount < nodeMembers.length;
    },
    [selectedIds]
  );

  return {
    selectedIds,
    toggleMember,
    toggleNode,
    selectAll,
    clearAll,
    isSelected,
    isNodeFullySelected,
    isNodePartiallySelected,
  };
}
