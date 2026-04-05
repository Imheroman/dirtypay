'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeOrderApi } from '../api';
import type { CreateStoreOrderRequest } from '../types';

interface CreateStoreOrderParams {
  storeId: string;
  request: CreateStoreOrderRequest;
}

export function useCreateStoreOrderMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: CreateStoreOrderParams) =>
      storeOrderApi.createOrder(Number(storeId), request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.orders.all(storeId) });
      toast.success('주문을 접수했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('주문을 접수하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
