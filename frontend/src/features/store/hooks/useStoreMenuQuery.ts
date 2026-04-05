'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';

export function useStoreMenuQuery(storeId: string, menuId: string) {
  return useQuery({
    queryKey: queryKeys.stores.menus.detail(storeId, menuId),
    queryFn: () => storeMenuApi.getMenu(Number(storeId), Number(menuId)),
    enabled: !!storeId && !!menuId,
  });
}
