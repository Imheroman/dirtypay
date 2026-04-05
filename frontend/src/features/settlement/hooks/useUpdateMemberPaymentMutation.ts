'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';

export function useUpdateMemberPaymentMutation(sessionId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orgMemberId, paidAmount }: { orgMemberId: number; paidAmount: number }) =>
      settlementApi.updateMemberPayment(sessionId, orgMemberId, paidAmount),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.settlement.session(String(sessionId)),
      });
    },
  });
}
