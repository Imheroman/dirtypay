'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { walletApi } from '../api';

export function useWalletQuery() {
  return useQuery({
    queryKey: queryKeys.wallet.me(),
    queryFn: walletApi.getMyWallet,
  });
}
