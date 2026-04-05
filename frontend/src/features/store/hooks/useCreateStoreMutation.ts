'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';
import type { CreateStoreRequest } from '../types';

export function useCreateStoreMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateStoreRequest) => storeApi.createStore(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.lists() });
      toast.success('가게를 등록했어요');
    },
    onError: () => {
      toast.error('가게를 등록하지 못했어요');
    },
  });
}
