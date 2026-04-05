'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

interface DeleteRoundParams {
  roundId: number;
  sessionId: number;
}

export function useDeleteRoundMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roundId }: DeleteRoundParams) =>
      roundApi.deleteRound(roundId),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.lists(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.all,
      });
      toast.success('라운드를 삭제했어요');
    },
    onError: () => {
      toast.error('라운드를 삭제하지 못했어요');
    },
  });
}
