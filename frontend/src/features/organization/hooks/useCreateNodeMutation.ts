'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { nodeApi } from '../api';
import type { CreateNodeRequest } from '../types';

interface CreateNodeParams {
  sessionId: number;
  request: CreateNodeRequest;
}

export function useCreateNodeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, request }: CreateNodeParams) =>
      nodeApi.createNode(sessionId, request),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
      toast.success('그룹을 추가했어요');
    },
    onError: () => {
      toast.error('그룹을 추가하지 못했어요');
    },
  });
}
