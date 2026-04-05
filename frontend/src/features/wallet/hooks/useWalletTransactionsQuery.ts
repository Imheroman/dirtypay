'use client';

import { useInfiniteQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { walletApi } from '../api';

const PAGE_SIZE = 20;

export function useWalletTransactionsQuery() {
  return useInfiniteQuery({
    queryKey: queryKeys.wallet.transactions(),
    queryFn: ({ pageParam = 0 }) =>
      walletApi.getTransactions({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.page + 1,
  });
}
