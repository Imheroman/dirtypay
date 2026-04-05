"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/keys";
import { storeApi } from "../api";

interface UseStoreQueryOptions {
  storeId: number;
  enabled?: boolean;
}

export function useStoreQuery({ storeId, enabled }: UseStoreQueryOptions) {
  return useQuery({
    queryKey: queryKeys.stores.detail(String(storeId)),
    queryFn: () => storeApi.getStore(storeId),
    enabled: enabled ?? true,
  });
}
