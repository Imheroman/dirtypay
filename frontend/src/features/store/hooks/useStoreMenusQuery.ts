'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';

export function useStoreMenusQuery(
  storeId: string,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: queryKeys.stores.menus.all(storeId),
    queryFn: () => storeMenuApi.getMenus(Number(storeId)),
    enabled: options?.enabled ?? !!storeId,
  });
}
