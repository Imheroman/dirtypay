'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';
import type { UpdateStoreMenuRequest } from '../types';

interface UpdateStoreMenuParams {
  storeId: string;
  menuId: string;
  request: UpdateStoreMenuRequest;
}

export function useUpdateStoreMenuMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, menuId, request }: UpdateStoreMenuParams) =>
      storeMenuApi.updateMenu(Number(storeId), Number(menuId), request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.menus.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success('메뉴를 수정했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('메뉴를 수정하지 못했어요');
    },
  });
}
