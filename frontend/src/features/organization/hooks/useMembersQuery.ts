'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { memberApi } from '../api';

interface UseMembersQueryOptions {
  enabled?: boolean;
}

export function useMembersQuery(sessionId: number, options?: UseMembersQueryOptions) {
  return useQuery({
    queryKey: queryKeys.organization.members(String(sessionId)),
    queryFn: () => memberApi.getMembers(sessionId),
    enabled: (options?.enabled ?? true) && !!sessionId,
  });
}
