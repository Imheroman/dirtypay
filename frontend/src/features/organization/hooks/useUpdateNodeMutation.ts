'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { nodeApi } from '../api';
import type { UpdateNodeRequest } from '../types';

interface UpdateNodeParams {
  id: number;
  sessionId: number;
  request: UpdateNodeRequest;
}

export function useUpdateNodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: UpdateNodeParams) =>
      nodeApi.updateNode(id, request),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      toast.success('그룹 정보를 수정했어요');
    },
    onError: () => {
      toast.error('그룹 정보를 수정하지 못했어요');
    },
  });
}
