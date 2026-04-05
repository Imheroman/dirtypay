"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/keys";
import { storeApi } from "../api";

interface UseStoresQueryOptions {
  sellerId: number;
  enabled?: boolean;
}

export function useStoresQuery({ sellerId, enabled }: UseStoresQueryOptions) {
  return useQuery({
    queryKey: queryKeys.sellers.storeList(sellerId),
    queryFn: () => storeApi.getStores(sellerId),
    enabled: enabled ?? true,
  });
}
