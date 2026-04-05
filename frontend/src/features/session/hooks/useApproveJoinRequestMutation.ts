'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { joinRequestApi } from '../api';
import type { ApproveJoinRequestPayload } from '../types';

export function useApproveJoinRequestMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      sessionId,
      requestId,
      payload,
    }: {
      sessionId: number;
      requestId: number;
      payload: ApproveJoinRequestPayload;
    }) => joinRequestApi.approve(sessionId, requestId, payload),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.joinRequests(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      toast.success('참여 요청을 승인했어요.');
    },
    onError: () => {
      toast.error('참여 요청을 승인하지 못했어요.');
    },
  });
}
