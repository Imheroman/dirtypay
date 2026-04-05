'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';

export function useStoreAverageRatingQuery(storeId: string) {
  return useQuery({
    queryKey: queryKeys.stores.reviews.rating(storeId),
    queryFn: () => storeReviewApi.getAverageRating(Number(storeId)),
    enabled: !!storeId,
  });
}
