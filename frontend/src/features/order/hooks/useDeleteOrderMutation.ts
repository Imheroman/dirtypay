'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { orderApi } from '../api';

interface DeleteOrderParams {
  orderId: number;
  roundId: number;
}

export function useDeleteOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orderId }: DeleteOrderParams) =>
      orderApi.deleteOrder(orderId),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.orders.byRound(String(roundId)),
      });
      toast.success('주문을 삭제했어요');
    },
    onError: () => {
      toast.error('주문을 삭제하지 못했어요');
    },
  });
}
