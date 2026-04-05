'use client';

import { Badge } from '@/components/ui/badge';
import { useJoinRequestsQuery } from '../hooks/useJoinRequestsQuery';

interface JoinRequestBadgeProps {
  sessionId: number;
  isOwner: boolean;
}

export function JoinRequestBadge({ sessionId, isOwner }: JoinRequestBadgeProps) {
  const { data: requests = [] } = useJoinRequestsQuery(sessionId, 'PENDING', {
    enabled: isOwner,
  });

  if (requests.length === 0) return null;

  return (
    <Badge variant="destructive" className="h-5 min-w-5 px-1.5 text-xs">
      {requests.length}
    </Badge>
  );
}
