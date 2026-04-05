'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

export function useIncludeParticipantMutation(roundId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (participantId: number) =>
      roundApi.includeParticipant(roundId, participantId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.participants(String(roundId)),
      });
    },
  });
}
