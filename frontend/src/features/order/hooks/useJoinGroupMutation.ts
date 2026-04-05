'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';

interface JoinGroupParams {
  groupId: number;
  roundId: number;
}

export function useJoinGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId }: JoinGroupParams) =>
      groupApi.joinGroup(groupId),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      toast.success('그룹에 참여했어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '그룹에 참여하지 못했어요');
    },
  });
}
