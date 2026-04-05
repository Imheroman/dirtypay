'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';
import type { RoundStatus } from '../types';

interface UpdateRoundStatusParams {
  roundId: number;
  sessionId: number;
  status: RoundStatus;
}

export function useUpdateRoundStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roundId, status }: UpdateRoundStatusParams) =>
      roundApi.updateRoundStatus(roundId, status),
    onSuccess: (_, { roundId, sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.detail(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.lists(String(sessionId)),
      });
      toast.success('라운드 상태를 변경했어요');
    },
    onError: () => {
      toast.error('라운드 상태를 변경하지 못했어요');
    },
  });
}
