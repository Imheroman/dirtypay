'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';

interface UseArchivedSessionsQueryOptions {
  enabled?: boolean;
}

export function useArchivedSessionsQuery(options?: UseArchivedSessionsQueryOptions) {
  return useQuery({
    queryKey: queryKeys.sessions.archived(),
    queryFn: sessionApi.getArchivedSessions,
    enabled: options?.enabled ?? true,
  });
}
