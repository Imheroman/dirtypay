"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/keys";
import { sellerApi } from "../api";

interface UseSellerQueryOptions {
  enabled?: boolean;
}

export function useSellerQuery(options?: UseSellerQueryOptions) {
  return useQuery({
    queryKey: queryKeys.sellers.me(),
    queryFn: sellerApi.getMySellerInfo,
    enabled: options?.enabled ?? true,
    retry: false,
  });
}
