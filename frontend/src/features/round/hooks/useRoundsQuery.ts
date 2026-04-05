'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

export function useRoundsQuery(sessionId: number) {
  return useQuery({
    queryKey: queryKeys.rounds.lists(String(sessionId)),
    queryFn: () => roundApi.getRounds(sessionId),
    enabled: !!sessionId,
  });
}
