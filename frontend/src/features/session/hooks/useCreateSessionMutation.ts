'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { sessionApi } from '../api';
import type { CreateSessionRequest } from '../types';

export function useCreateSessionMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateSessionRequest) => sessionApi.createSession(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sessions.all });
      toast.success('세션을 만들었어요');
    },
    onError: () => {
      toast.error('세션을 만들지 못했어요');
    },
  });
}
