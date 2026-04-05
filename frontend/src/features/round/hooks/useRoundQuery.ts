'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';

export function useRoundQuery(id: string) {
  return useQuery({
    queryKey: queryKeys.rounds.detail(id),
    queryFn: () => roundApi.getRound(Number(id)),
    enabled: !!id,
  });
}
