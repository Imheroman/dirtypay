'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeOrderApi } from '../api';

export function useStoreOrderQuery(storeId: string, orderId: string) {
  return useQuery({
    queryKey: queryKeys.stores.orders.detail(storeId, orderId),
    queryFn: () => storeOrderApi.getOrder(Number(storeId), Number(orderId)),
    enabled: !!storeId && !!orderId,
  });
}
