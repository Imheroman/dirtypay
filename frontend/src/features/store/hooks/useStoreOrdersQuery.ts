'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeOrderApi } from '../api';

export function useStoreOrdersQuery(
  storeId: string,
  params?: Record<string, unknown>,
) {
  return useQuery({
    queryKey: queryKeys.stores.orders.list(storeId, params),
    queryFn: () => storeOrderApi.getOrders(Number(storeId), params),
    enabled: !!storeId,
  });
}
