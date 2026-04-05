'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import type { ApiResponse } from '@/types/api';
import { roundApi } from '../api';
import type { CreateRoundRequest } from '../types';

interface CreateRoundParams {
  sessionId: number;
  request: CreateRoundRequest;
}

export function useCreateRoundMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, request }: CreateRoundParams) =>
      roundApi.createRound(sessionId, request),
    onSuccess: (_, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.lists(String(sessionId)),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.sessions.all,
      });
      toast.success('라운드를 추가했어요');
    },
    onError: (error) => {
      const axiosError = error as AxiosError<ApiResponse<unknown>>;
      const serverMessage = axiosError.response?.data?.error?.message;
      toast.error(serverMessage || '라운드를 추가하지 못했어요');
    },
  });
}
