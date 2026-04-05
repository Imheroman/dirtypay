'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { nodeApi } from '../api';

export function useNodesQuery(sessionId: number) {
  return useQuery({
    queryKey: queryKeys.organization.nodes(String(sessionId)),
    queryFn: () => nodeApi.getNodes(sessionId),
    enabled: !!sessionId,
  });
}
