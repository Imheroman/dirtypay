'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';

interface DeleteStoreReviewParams {
  storeId: string;
  reviewId: string;
}

export function useDeleteStoreReviewMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, reviewId }: DeleteStoreReviewParams) =>
      storeReviewApi.deleteReview(Number(storeId), Number(reviewId)),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.rating(storeId) });
      toast.success('리뷰를 삭제했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('리뷰를 삭제하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
