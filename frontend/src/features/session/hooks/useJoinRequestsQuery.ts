'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { joinRequestApi } from '../api';
import type { JoinRequestStatus } from '../types';

interface UseJoinRequestsQueryOptions {
  enabled?: boolean;
}

export function useJoinRequestsQuery(
  sessionId: number,
  status?: JoinRequestStatus,
  options?: UseJoinRequestsQueryOptions
) {
  return useQuery({
    queryKey: queryKeys.sessions.joinRequests(String(sessionId), status),
    queryFn: () => joinRequestApi.list(sessionId, status),
    enabled: (options?.enabled ?? true) && !!sessionId,
  });
}
