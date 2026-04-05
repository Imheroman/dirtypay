'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';

interface DeleteStoreMenuParams {
  storeId: string;
  menuId: string;
}

export function useDeleteStoreMenuMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, menuId }: DeleteStoreMenuParams) =>
      storeMenuApi.deleteMenu(Number(storeId), Number(menuId)),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.menus.all(storeId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success('메뉴를 삭제했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('메뉴를 삭제하지 못했어요');
    },
  });
}
