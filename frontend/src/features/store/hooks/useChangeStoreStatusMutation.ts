'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';
import type { ChangeStoreStatusRequest } from '../types';

interface ChangeStoreStatusParams {
  storeId: number;
  request: ChangeStoreStatusRequest;
}

export function useChangeStoreStatusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: ChangeStoreStatusParams) =>
      storeApi.changeStoreStatus(storeId, request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.detail(String(storeId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.lists() });
      toast.success('가게 상태를 변경했어요');
    },
    onError: () => {
      toast.error('가게 상태를 변경하지 못했어요');
    },
  });
}
