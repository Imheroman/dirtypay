'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { walletApi } from '../api';
import type { TransferRequest } from '../types';

export function useTransferMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: TransferRequest) => walletApi.transfer(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.me() });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.transactions() });
      toast.success('송금했어요');
    },
    onError: () => {
      toast.error('송금에 실패했어요. 다시 시도해 주세요.');
    },
  });
}
