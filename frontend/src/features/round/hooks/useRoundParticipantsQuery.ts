'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

export function useRoundParticipantsQuery(roundId: number) {
  return useQuery({
    queryKey: queryKeys.rounds.participants(String(roundId)),
    queryFn: () => roundApi.getParticipants(roundId),
    enabled: !!roundId,
  });
}
