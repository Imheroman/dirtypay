'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { settlementApi } from '../api';

export function useGroupOrdersQuery(roundId: number, groupId: number) {
  return useQuery({
    queryKey: queryKeys.settlement.group(String(roundId), String(groupId)),
    queryFn: () => settlementApi.getGroupOrders(roundId, groupId),
    enabled: !!roundId && !!groupId,
  });
}
