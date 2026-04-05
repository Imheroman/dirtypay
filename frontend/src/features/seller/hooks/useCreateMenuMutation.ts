"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/hooks/queries/keys";
import { storeMenuApi } from "../api";
import type { CreateMenuRequest } from "../types";

interface CreateMenuParams {
  storeId: number;
  request: CreateMenuRequest;
}

export function useCreateMenuMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: CreateMenuParams) =>
      storeMenuApi.createMenu(storeId, request),
    onSuccess: (_, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.storeMenus.byStore(storeId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.stores.detail(String(storeId)),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.storeMenus.all });
      toast.success("메뉴를 추가했어요");
    },
    onError: () => {
      toast.error("메뉴를 추가하지 못했어요");
    },
  });
}
