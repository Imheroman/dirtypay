'use client';

import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import { EmptyState } from '@/components/common/EmptyState';
import { UsersIcon } from '@/components/common/Icons';
import { useNodesQuery } from '../hooks/useNodesQuery';
import { isUnassignedNode } from '../types';
import { TreeNode } from './TreeNode';

interface TreeViewProps {
  sessionId: number;
}

function TreeViewSkeleton() {
  return (
    <div className="space-y-1">
      {[1, 2, 3].map((i) => (
        <div key={i} className="flex items-center gap-2 px-2 py-1.5">
          <Skeleton className="h-4 w-4 shrink-0" />
          <Skeleton className="h-4 w-4 shrink-0" />
          <Skeleton className="h-4 w-24" />
          <Skeleton className="ml-auto h-5 w-10 rounded-md" />
        </div>
      ))}
      <div className="pl-6 space-y-1">
        {[1, 2].map((i) => (
          <div key={i} className="flex items-center gap-2 px-2 py-1.5">
            <Skeleton className="h-3.5 w-3.5 shrink-0" />
            <Skeleton className="h-4 w-20" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function TreeView({ sessionId }: TreeViewProps) {
  const { data, isLoading, error, refetch } = useNodesQuery(sessionId);

  if (isLoading) {
    return <TreeViewSkeleton />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="조직 정보를 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  if (!data || data.length === 0) {
    return (
      <EmptyState
        icon={<UsersIcon className="h-6 w-6 text-muted-foreground" />}
        title="아직 등록된 조직이 없어요"
        description="그룹을 만들어 멤버를 추가해보세요"
      />
    );
  }

  const regularNodes = data.filter((n) => !isUnassignedNode(n));
  const unassignedNodes = data.filter((n) => isUnassignedNode(n));

  return (
    <div className="space-y-4">
      {/* 배정된 그룹 */}
      <div className="space-y-1">
        {regularNodes.map((node) => (
          <TreeNode key={node.id} node={node} defaultExpanded />
        ))}
      </div>

      {/* 미배정 그룹 섹션 */}
      {unassignedNodes.length > 0 && (
        <div className="rounded-lg border border-dashed border-muted-foreground/30 bg-muted/30 p-2">
          <p className="mb-1.5 px-2 text-xs font-medium text-muted-foreground">
            미배정
          </p>
          <div className="space-y-1">
            {unassignedNodes.map((node) => (
              <TreeNode key={node.id} node={node} defaultExpanded />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
