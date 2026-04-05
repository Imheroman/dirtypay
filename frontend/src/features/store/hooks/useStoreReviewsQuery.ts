'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';

export function useStoreReviewsQuery(storeId: string) {
  return useQuery({
    queryKey: queryKeys.stores.reviews.all(storeId),
    queryFn: () => storeReviewApi.getReviews(Number(storeId)),
    enabled: !!storeId,
  });
}
