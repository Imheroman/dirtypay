'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';
import type { SettlementStrategy } from '../types';

export function useRoundSettlementQuery(
  roundId: number,
  strategy?: SettlementStrategy
) {
  return useQuery({
    queryKey: [...queryKeys.settlement.round(String(roundId)), strategy],
    queryFn: () => settlementApi.getRoundSettlement(roundId, strategy),
    enabled: !!roundId,
  });
}
