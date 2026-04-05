"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/hooks/queries/keys";
import { storeMenuApi } from "../api";

interface DeleteMenuParams {
  menuId: number;
  storeId: number;
}

export function useDeleteMenuMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ menuId }: DeleteMenuParams) =>
      storeMenuApi.deleteMenu(menuId),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.storeMenus.byStore(storeId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.stores.detail(String(storeId)),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success("메뉴를 삭제했어요");
    },
    onError: () => {
      toast.error("메뉴를 삭제하지 못했어요");
    },
  });
}
