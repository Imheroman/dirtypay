'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeReviewApi } from '../api';
import type { CreateStoreReviewRequest } from '../types';

interface CreateStoreReviewParams {
  storeId: string;
  request: CreateStoreReviewRequest;
}

export function useCreateStoreReviewMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: CreateStoreReviewParams) =>
      storeReviewApi.createReview(Number(storeId), request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.reviews.rating(storeId) });
      toast.success('리뷰를 등록했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('리뷰를 등록하지 못했어요. 다시 시도해 주세요.');
    },
  });
}
