'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';

export function useStoreStatisticsQuery(storeId: string, params?: Record<string, unknown>) {
  return useQuery({
    queryKey: queryKeys.stores.statistics(storeId),
    queryFn: () => storeApi.getStatistics(Number(storeId), params),
    enabled: !!storeId,
  });
}
