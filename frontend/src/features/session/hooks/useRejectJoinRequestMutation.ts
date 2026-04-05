'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { joinRequestApi } from '../api';

export function useRejectJoinRequestMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      sessionId,
      requestId,
    }: {
      sessionId: number;
      requestId: number;
    }) => joinRequestApi.reject(sessionId, requestId),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.joinRequests(String(sessionId)),
      });
      toast.success('참여 요청을 거절했어요.');
    },
    onError: () => {
      toast.error('참여 요청을 거절하지 못했어요.');
    },
  });
}
