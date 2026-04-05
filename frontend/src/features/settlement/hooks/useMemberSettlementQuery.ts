'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';
import type { SettlementStrategy } from '../types';

export function useMemberSettlementQuery(
  sessionId: number,
  orgMemberId: number,
  strategy?: SettlementStrategy
) {
  return useQuery({
    queryKey: [...queryKeys.settlement.member(String(sessionId), String(orgMemberId)), strategy],
    queryFn: () => settlementApi.getMemberSettlement(sessionId, orgMemberId, strategy),
    enabled: !!sessionId && !!orgMemberId,
  });
}
