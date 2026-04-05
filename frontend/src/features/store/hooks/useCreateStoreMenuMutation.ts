'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';
import type { CreateStoreMenuRequest } from '../types';

interface CreateStoreMenuParams {
  storeId: string;
  request: CreateStoreMenuRequest;
}

export function useCreateStoreMenuMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: CreateStoreMenuParams) =>
      storeMenuApi.createMenu(Number(storeId), request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.menus.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success('메뉴를 등록했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('메뉴를 등록하지 못했어요');
    },
  });
}
