'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';

export function useRoundGroupsQuery(roundId: string) {
  return useQuery({
    queryKey: queryKeys.roundGroups.byRound(roundId),
    queryFn: () => groupApi.getGroups(Number(roundId)),
    enabled: !!roundId,
  });
}
