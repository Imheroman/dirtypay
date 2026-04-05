'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

export function useExcludeParticipantMutation(roundId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (participantId: number) =>
      roundApi.excludeParticipant(roundId, participantId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.participants(String(roundId)),
      });
    },
  });
}
