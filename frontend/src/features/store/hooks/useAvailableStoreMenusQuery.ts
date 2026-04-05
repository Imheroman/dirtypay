'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';

export function useAvailableStoreMenusQuery(
  storeId: string,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: queryKeys.stores.menus.available(storeId),
    queryFn: () => storeMenuApi.getAvailableMenus(Number(storeId)),
    enabled: options?.enabled ?? !!storeId,
  });
}
