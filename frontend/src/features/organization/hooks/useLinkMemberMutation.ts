'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { memberApi } from '../api';

interface LinkMemberParams {
  memberId: number;
  sessionId: number;
}

export function useLinkMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ memberId }: LinkMemberParams) =>
      memberApi.linkMember(memberId),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('내 계정을 연결했어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '계정을 연결하지 못했어요');
    },
  });
}
