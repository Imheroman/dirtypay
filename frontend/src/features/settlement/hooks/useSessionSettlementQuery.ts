'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';
import type { SettlementStrategy } from '../types';

export function useSessionSettlementQuery(
  sessionId: number,
  strategy?: SettlementStrategy
) {
  return useQuery({
    queryKey: [...queryKeys.settlement.session(String(sessionId)), strategy],
    queryFn: () => settlementApi.getSessionSettlement(sessionId, strategy),
    enabled: !!sessionId,
  });
}
