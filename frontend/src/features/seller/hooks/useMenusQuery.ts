"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/keys";
import { storeMenuApi } from "../api";

interface UseMenusQueryOptions {
  storeId: number;
  enabled?: boolean;
}

export function useMenusQuery({ storeId, enabled }: UseMenusQueryOptions) {
  return useQuery({
    queryKey: queryKeys.storeMenus.byStore(storeId),
    queryFn: () => storeMenuApi.getMenus(storeId),
    enabled: enabled ?? true,
  });
}
