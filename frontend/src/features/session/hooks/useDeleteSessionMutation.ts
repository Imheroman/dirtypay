'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';

export function useDeleteSessionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => sessionApi.deleteSession(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sessions.all });
      toast.success('세션을 삭제했어요');
    },
    onError: () => {
      toast.error('세션을 삭제하지 못했어요');
    },
  });
}
