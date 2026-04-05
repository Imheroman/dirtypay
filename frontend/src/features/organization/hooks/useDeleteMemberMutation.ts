'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { memberApi } from '../api';

interface DeleteMemberParams {
  id: number;
  sessionId: number;
}

export function useDeleteMemberMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id }: DeleteMemberParams) => memberApi.deleteMember(id),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('멤버를 삭제했어요');
    },
    onError: () => {
      toast.error('멤버를 삭제하지 못했어요');
    },
  });
}
