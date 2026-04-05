'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import type { PaginationParams } from '@/types/api';
import { storeApi } from '../api';

export function useStoresQuery(params?: PaginationParams & { scope?: string }) {
  return useQuery({
    queryKey: queryKeys.stores.list(params as Record<string, unknown>),
    queryFn: () => storeApi.getStores(params),
  });
}
