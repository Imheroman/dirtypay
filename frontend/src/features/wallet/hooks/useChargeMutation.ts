'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { walletApi } from '../api';
import type { ChargeRequest } from '../types';

export function useChargeMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ChargeRequest) => walletApi.charge(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.me() });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.transactions() });
      toast.success('충전했어요');
    },
    onError: () => {
      toast.error('충전에 실패했어요. 다시 시도해 주세요.');
    },
  });
}
