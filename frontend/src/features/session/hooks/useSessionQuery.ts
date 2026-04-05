'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';

interface UseSessionQueryOptions {
  enabled?: boolean;
}

export function useSessionQuery(id: number, options?: UseSessionQueryOptions) {
  return useQuery({
    queryKey: queryKeys.sessions.detail(String(id)),
    queryFn: () => sessionApi.getSession(id),
    enabled: options?.enabled ?? true,
  });
}
