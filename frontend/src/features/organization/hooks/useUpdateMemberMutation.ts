'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { memberApi } from '../api';
import type { UpdateMemberRequest } from '../types';

interface UpdateMemberParams {
  id: number;
  sessionId: number;
  request: UpdateMemberRequest;
}

export function useUpdateMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: UpdateMemberParams) =>
      memberApi.updateMember(id, request),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('멤버 정보를 수정했어요');
    },
    onError: () => {
      toast.error('멤버 정보를 수정하지 못했어요');
    },
  });
}
