'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';
import type { UpdateRoundRequest } from '../types';

interface UpdateRoundParams {
  roundId: number;
  sessionId: number;
  request: UpdateRoundRequest;
}

export function useUpdateRoundMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roundId, request }: UpdateRoundParams) =>
      roundApi.updateRound(roundId, request),
    onSuccess: (_, { roundId, sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.lists(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.detail(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.storeMenus.all,
      });
      toast.success('라운드를 수정했어요');
    },
    onError: () => {
      toast.error('라운드를 수정하지 못했어요');
    },
  });
}
