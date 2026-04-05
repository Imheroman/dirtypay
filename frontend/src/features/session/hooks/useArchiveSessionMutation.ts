'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';

export function useArchiveSessionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => sessionApi.archiveSession(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sessions.all });
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.detail(String(id)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.archived(),
      });
      toast.success('세션을 완료했어요');
    },
    onError: () => {
      toast.error('세션을 완료하지 못했어요');
    },
  });
}
