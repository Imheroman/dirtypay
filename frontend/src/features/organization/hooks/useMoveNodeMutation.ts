'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { nodeApi } from '../api';
import type { MoveNodeRequest } from '../types';

export function useMoveNodeMutation(sessionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ nodeId, request }: { nodeId: number; request: MoveNodeRequest }) =>
      nodeApi.moveNode(nodeId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.organization.nodes(String(sessionId)),
      });
    },
  });
}
