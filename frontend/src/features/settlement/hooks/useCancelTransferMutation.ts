'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';

export function useCancelTransferMutation(sessionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (transferId: number) => settlementApi.cancelTransfer(transferId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.settlement.transfers(String(sessionId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.settlement.session(String(sessionId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.me() });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.transactions() });
      toast.success('송금을 취소했어요');
    },
    onError: () => {
      toast.error('취소에 실패했어요. 다시 시도해 주세요.');
    },
  });
}
