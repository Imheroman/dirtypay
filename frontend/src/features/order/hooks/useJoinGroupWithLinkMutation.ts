'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';
import { memberApi } from '@/features/organization/api';

interface JoinGroupWithLinkParams {
  groupId: number;
  memberId: number;
  sessionId: number;
  roundId: number;
  currentGroupId?: number;
}

export function useJoinGroupWithLinkMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ groupId, memberId, currentGroupId }: JoinGroupWithLinkParams) => {
      if (currentGroupId) {
        // 기존 그룹 있음 → changeGroup (atomic move)
        await groupApi.changeGroup(currentGroupId, { toGroupId: groupId });
      } else {
        // 기존 그룹 없음 → joinGroup
        try {
          await groupApi.joinGroup(groupId);
        } catch (error) {
          const isNotFound =
            error instanceof AxiosError &&
            error.response?.status === 404;

          if (!isNotFound) throw error;

          await memberApi.linkMember(memberId);
          await groupApi.joinGroup(groupId);
        }
      }
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
