'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { memberApi } from '../api';
import type { CreateMemberRequest } from '../types';

interface CreateMemberParams {
  sessionId: number;
  request: CreateMemberRequest;
}

export function useCreateMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, request }: CreateMemberParams) =>
      memberApi.createMember(sessionId, request),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('멤버를 추가했어요');
    },
    onError: () => {
      toast.error('멤버를 추가하지 못했어요');
    },
  });
}
