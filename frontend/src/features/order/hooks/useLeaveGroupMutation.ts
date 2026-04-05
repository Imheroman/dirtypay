'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';

interface LeaveGroupParams {
  groupId: number;
  roundId: number;
}

export function useLeaveGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId }: LeaveGroupParams) =>
      groupApi.leaveGroup(groupId),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.participants(String(roundId)),
      });
      toast.success('그룹에서 나왔어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '그룹에서 나가지 못했어요');
    },
  });
}
