'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';

export function useStoreReviewQuery(storeId: string, reviewId: string) {
  return useQuery({
    queryKey: queryKeys.stores.reviews.detail(storeId, reviewId),
    queryFn: () => storeReviewApi.getReview(Number(storeId), Number(reviewId)),
    enabled: !!storeId && !!reviewId,
  });
}
