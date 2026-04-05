'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';
import type { UpdateSessionRequest } from '../types';

interface UpdateSessionVariables {
  id: number;
  request: UpdateSessionRequest;
}

export function useUpdateSessionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: UpdateSessionVariables) =>
      sessionApi.updateSession(id, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sessions.all });
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.detail(String(variables.id)),
      });
      toast.success('세션 정보를 수정했어요');
    },
    onError: () => {
      toast.error('세션 정보를 수정하지 못했어요');
    },
  });
}
