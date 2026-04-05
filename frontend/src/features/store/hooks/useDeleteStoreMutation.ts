'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeApi } from '../api';

interface DeleteStoreParams {
  storeId: number;
}

export function useDeleteStoreMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId }: DeleteStoreParams) => storeApi.deleteStore(storeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.lists() });
      toast.success('가게를 삭제했어요');
    },
    onError: () => {
      toast.error('가게를 삭제하지 못했어요');
    },
  });
}
