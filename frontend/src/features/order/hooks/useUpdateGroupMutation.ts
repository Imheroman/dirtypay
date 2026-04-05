'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';
import type { UpdateGroupRequest } from '../types';

interface UpdateGroupParams {
  groupId: number;
  roundId: number;
  request: UpdateGroupRequest;
}

export function useUpdateGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, request }: UpdateGroupParams) =>
      groupApi.updateGroup(groupId, request),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      toast.success('그룹 이름을 변경했어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '그룹 이름을 변경하지 못했어요');
    },
  });
}
