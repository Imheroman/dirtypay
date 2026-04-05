'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';

export function useStoreQuery(storeId: string) {
  return useQuery({
    queryKey: queryKeys.stores.detail(storeId),
    queryFn: () => storeApi.getStore(Number(storeId)),
    enabled: !!storeId,
  });
}
