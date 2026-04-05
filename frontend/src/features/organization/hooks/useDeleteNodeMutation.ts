'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { nodeApi } from '../api';

interface DeleteNodeParams {
  id: number;
  sessionId: number;
}

export function useDeleteNodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id }: DeleteNodeParams) => nodeApi.deleteNode(id),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.members(String(sessionId)),
      });
      toast.success('그룹을 삭제했어요');
    },
    onError: () => {
      toast.error('그룹을 삭제하지 못했어요');
    },
  });
}
