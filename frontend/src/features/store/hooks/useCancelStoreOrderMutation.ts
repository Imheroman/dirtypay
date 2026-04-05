'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeOrderApi } from '../api';

interface CancelStoreOrderParams {
  storeId: string;
  orderId: string;
}

export function useCancelStoreOrderMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, orderId }: CancelStoreOrderParams) =>
      storeOrderApi.cancelOrder(Number(storeId), Number(orderId)),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.orders.all(storeId) });
      toast.success('주문을 취소했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('주문을 취소하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
