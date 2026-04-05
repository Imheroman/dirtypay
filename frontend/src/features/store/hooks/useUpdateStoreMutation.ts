'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';
import type { UpdateStoreRequest } from '../types';

interface UpdateStoreParams {
  storeId: number;
  request: UpdateStoreRequest;
}

export function useUpdateStoreMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: UpdateStoreParams) =>
      storeApi.updateStore(storeId, request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.detail(String(storeId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.lists() });
      toast.success('가게 정보를 수정했어요');
    },
    onError: () => {
      toast.error('가게를 수정하지 못했어요');
    },
  });
}
