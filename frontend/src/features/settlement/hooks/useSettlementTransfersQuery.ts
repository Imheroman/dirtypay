'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';

export function useSettlementTransfersQuery(sessionId: number) {
  return useQuery({
    queryKey: queryKeys.settlement.transfers(String(sessionId)),
    queryFn: () => settlementApi.getTransfers(sessionId),
    enabled: !!sessionId,
  });
}
