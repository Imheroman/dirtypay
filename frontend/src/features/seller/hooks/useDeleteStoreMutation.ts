"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/hooks/queries/keys";
import { storeApi } from "../api";

export function useDeleteStoreMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (storeId: number) => storeApi.deleteStore(storeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sellers.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.stores.all });
      toast.success("매장을 삭제했어요");
    },
    onError: () => {
      toast.error("매장을 삭제하지 못했어요");
    },
  });
}
