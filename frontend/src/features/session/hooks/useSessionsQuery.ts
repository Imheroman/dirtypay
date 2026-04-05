'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';

interface UseSessionsQueryOptions {
  enabled?: boolean;
}

export function useSessionsQuery(options?: UseSessionsQueryOptions) {
  return useQuery({
    queryKey: queryKeys.sessions.lists(),
    queryFn: sessionApi.getSessions,
    enabled: options?.enabled ?? true,
  });
}
