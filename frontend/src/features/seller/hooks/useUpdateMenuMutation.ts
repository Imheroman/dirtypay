"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/hooks/queries/keys";
import { storeMenuApi } from "../api";
import type { UpdateMenuRequest } from "../types";

interface UpdateMenuParams {
  menuId: number;
  storeId: number;
  request: UpdateMenuRequest;
}

export function useUpdateMenuMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ menuId, request }: UpdateMenuParams) =>
      storeMenuApi.updateMenu(menuId, request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.storeMenus.byStore(storeId),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success("메뉴를 수정했어요");
    },
    onError: () => {
      toast.error("메뉴를 수정하지 못했어요");
    },
  });
}
