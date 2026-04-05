'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';
import type { CreateTransferRequest } from '../types';

export function useSettlementTransferMutation(sessionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orgMemberId, request }: { orgMemberId: number; request: CreateTransferRequest }) =>
      settlementApi.createTransfer(sessionId, orgMemberId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.settlement.transfers(String(sessionId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.settlement.session(String(sessionId)) });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.me() });
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet.transactions() });
      toast.success('송금했어요');
    },
    onError: () => {
      toast.error('송금에 실패했어요. 다시 시도해 주세요.');
    },
  });
}
