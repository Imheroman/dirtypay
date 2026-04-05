'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeOrderApi } from '../api';
import type { ChangeStoreOrderStatusRequest } from '../types';

interface ChangeStoreOrderStatusParams {
  storeId: string;
  orderId: string;
  request: ChangeStoreOrderStatusRequest;
}

export function useChangeStoreOrderStatusMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, orderId, request }: ChangeStoreOrderStatusParams) =>
      storeOrderApi.changeStatus(Number(storeId), Number(orderId), request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.orders.all(storeId) });
      toast.success('주문 상태를 변경했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('주문 상태를 변경하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
