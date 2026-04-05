'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';

interface MoveGroupParams {
  sourceGroupId: number;
  targetGroupId: number;
  memberId: number;
  sessionId: number;
  roundId: number;
}

export function useMoveGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ sourceGroupId, targetGroupId }: MoveGroupParams) => {
      await groupApi.changeGroup(sourceGroupId, { toGroupId: targetGroupId });
    },
    onSuccess: (_, { roundId, sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.participants(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('그룹을 이동했어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '그룹 이동에 실패했어요. 다시 시도해 주세요.');
    },
  });
}
