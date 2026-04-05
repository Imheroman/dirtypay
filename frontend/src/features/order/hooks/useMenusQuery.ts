'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '@/features/store/api';
import type { StoreMenu } from '@/features/store/types';

/**
 * 가게의 판매 가능한 메뉴 목록 조회 Hook
 * @param storeId - 가게 ID
 * @returns 메뉴 목록 Query 결과
 */
export function useMenusQuery(storeId: number | undefined) {
  return useQuery({
    queryKey: queryKeys.storeMenus.byStore(storeId!),
    queryFn: () => storeMenuApi.getAvailableMenus(storeId!),
    enabled: !!storeId,
  });
}

/**
 * 카테고리별 그룹화된 메뉴 목록 조회 Hook
 * @param storeId - 가게 ID
 * @returns 카테고리별 그룹화된 메뉴 목록
 */
export function useGroupedMenusQuery(storeId: number | undefined) {
  return useQuery({
    queryKey: [...queryKeys.storeMenus.byStore(storeId!), 'grouped'],
    queryFn: () => storeMenuApi.getAvailableMenus(storeId!),
    enabled: !!storeId,
    select: (data) => {
      const grouped = data.reduce(
        (acc, menu) => {
          const category = menu.category || '기타';
          if (!acc[category]) acc[category] = [];
          acc[category].push(menu);
          return acc;
        },
        {} as Record<string, StoreMenu[]>
      );

      return {
        all: data,
        grouped,
      };
    },
  });
}
