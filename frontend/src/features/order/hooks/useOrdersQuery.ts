'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { orderApi } from '../api';
import { groupOrdersByCategory } from '../utils';

/**
 * 라운드의 주문 목록 조회 Hook
 * @param roundId - 라운드 ID
 * @returns 주문 목록 Query 결과
 */
export function useOrdersQuery(roundId: string) {
  return useQuery({
    queryKey: queryKeys.orders.byRound(roundId),
    queryFn: () => orderApi.getOrders(Number(roundId)),
    enabled: !!roundId,
  });
}

/**
 * 카테고리별 그룹화된 주문 목록 조회 Hook
 * @param roundId - 라운드 ID
 * @returns 카테고리별 그룹화된 주문 목록
 */
export function useGroupedOrdersQuery(roundId: string) {
  return useQuery({
    queryKey: [...queryKeys.orders.byRound(roundId), 'grouped'],
    queryFn: () => orderApi.getOrders(Number(roundId)),
    enabled: !!roundId,
    select: (data) => ({
      orders: data,
      grouped: groupOrdersByCategory(data),
      totalAmount: data.reduce((sum, order) => sum + order.totalPrice, 0),
    }),
  });
}
