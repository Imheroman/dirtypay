'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { orderApi } from '../api';
import type { CreateOrderRequest } from '../types';

interface CreateOrderParams {
  roundId: number;
  request: CreateOrderRequest;
}

export function useCreateOrderMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roundId, request }: CreateOrderParams) =>
      orderApi.createOrder(roundId, request),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.orders.byRound(String(roundId)),
      });
      toast.success('주문을 추가했어요');
    },
    onError: () => {
      toast.error('주문을 추가하지 못했어요');
    },
  });
}
