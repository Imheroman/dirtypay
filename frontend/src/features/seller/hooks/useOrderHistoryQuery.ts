"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/keys";
import { storeOrderApi } from "../api";

interface UseOrderHistoryQueryOptions {
  storeId: number;
  enabled?: boolean;
}

export function useOrderHistoryQuery({
  storeId,
  enabled,
}: UseOrderHistoryQueryOptions) {
  return useQuery({
    queryKey: queryKeys.storeOrders.byStore(storeId),
    queryFn: () => storeOrderApi.getOrders(storeId),
    enabled: enabled ?? true,
  });
}
