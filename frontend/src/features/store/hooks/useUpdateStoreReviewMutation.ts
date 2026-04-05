'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';
import type { UpdateStoreReviewRequest } from '../types';

interface UpdateStoreReviewParams {
  storeId: string;
  reviewId: string;
  request: UpdateStoreReviewRequest;
}

export function useUpdateStoreReviewMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, reviewId, request }: UpdateStoreReviewParams) =>
      storeReviewApi.updateReview(Number(storeId), Number(reviewId), request),
    onSuccess: (_, { storeId, reviewId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.detail(storeId, reviewId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.rating(storeId) });
      toast.success('리뷰를 수정했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('리뷰를 수정하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
