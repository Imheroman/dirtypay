'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';

export function useStorePopularMenusQuery(storeId: string, params?: Record<string, unknown>) {
  return useQuery({
    queryKey: queryKeys.stores.popularMenus(storeId),
    queryFn: () => storeApi.getPopularMenus(Number(storeId), params),
    enabled: !!storeId,
  });
}
