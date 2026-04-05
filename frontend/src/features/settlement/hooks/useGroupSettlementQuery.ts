'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';
import type { SettlementStrategy } from '../types';

export function useGroupSettlementQuery(
  roundId: number,
  groupId: number,
  strategy?: SettlementStrategy
) {
  return useQuery({
    queryKey: [...queryKeys.settlement.groupAmounts(String(roundId), String(groupId)), strategy],
    queryFn: () => settlementApi.getGroupSettlement(roundId, groupId, strategy),
    enabled: !!roundId && !!groupId,
  });
}
