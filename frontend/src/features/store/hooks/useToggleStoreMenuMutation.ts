'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { storeMenuApi } from '../api';

interface ToggleStoreMenuParams {
  storeId: string;
  menuId: string;
}

export function useToggleStoreMenuMutation(onSuccess?: () => void) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, menuId }: ToggleStoreMenuParams) =>
      storeMenuApi.toggleMenuAvailability(Number(storeId), Number(menuId)),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.menus.all(storeId) });
      toast.success('메뉴 판매 상태를 변경했어요');
      onSuccess?.();
    },
    onError: () => {
      toast.error('메뉴 판매 상태를 변경하지 못했어요');
    },
  });
}
