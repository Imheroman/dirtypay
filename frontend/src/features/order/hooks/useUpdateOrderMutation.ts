'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { orderApi } from '../api';
import type { UpdateOrderRequest } from '../types';

interface UpdateOrderParams {
  orderId: number;
  roundId: number;
  request: UpdateOrderRequest;
}

export function useUpdateOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orderId, request }: UpdateOrderParams) =>
      orderApi.updateOrder(orderId, request),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.orders.byRound(String(roundId)),
      });
      toast.success('주문을 수정했어요');
    },
    onError: () => {
      toast.error('주문을 수정하지 못했어요');
    },
  });
}
