"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/hooks/queries/keys";
import { storeApi } from "../api";
import type { ChangeStoreStateRequest } from "../types";

interface ChangeStoreStateParams {
  storeId: number;
  request: ChangeStoreStateRequest;
}

const statusLabel: Record<string, string> = {
  OPEN: "영업 중",
  TEMPORARILY_CLOSED: "임시 휴업",
  CLOSED: "운영 종료",
};

export function useChangeStoreStateMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ storeId, request }: ChangeStoreStateParams) =>
      storeApi.changeStoreState(storeId, request),
    onSuccess: (data, { storeId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.stores.detail(String(storeId)),
      });
      queryClient.invalidateQueries({ queryKey: queryKeys.sellers.all });
      toast.success(
        `매장 상태를 "${statusLabel[data.status] ?? data.status}"(으)로 변경했어요`,
      );
    },
    onError: () => {
      toast.error("매장 상태를 변경하지 못했어요");
    },
  });
}
