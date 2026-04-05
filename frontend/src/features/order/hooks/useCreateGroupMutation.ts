'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';
import type { CreateGroupRequest } from '../types';

interface CreateGroupParams {
  roundId: number;
  request: CreateGroupRequest;
  currentGroupId?: number;
}

export function useCreateGroupMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ roundId, request, currentGroupId }: CreateGroupParams) => {
      const group = await groupApi.createGroup(roundId, request);
      // 백엔드 자동 참여 + 기존 그룹 존재 시 → 기존 그룹 탈퇴
      if (currentGroupId && group.isParticipating) {
        await groupApi.leaveGroup(currentGroupId);
      }
      return group;
    },
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.participants(String(roundId)),
      });
      toast.success('그룹을 만들었어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '그룹을 만들지 못했어요');
    },
  });
}
