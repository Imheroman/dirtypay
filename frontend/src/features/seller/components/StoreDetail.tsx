"use client";

import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Skeleton } from "@/components/ui/skeleton";
import { useStoreQuery } from "../hooks";
import { StoreInfo } from "./StoreInfo";
import { MenuList } from "./MenuList";
import { OrderHistory } from "./OrderHistory";

interface StoreDetailProps {
  storeId: number;
}

export function StoreDetail({ storeId }: StoreDetailProps) {
  const { data: store, isLoading, error, refetch } = useStoreQuery({ storeId });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-[200px] rounded-lg" />
        <Skeleton className="h-[300px] rounded-lg" />
        <Skeleton className="h-[200px] rounded-lg" />
      </div>
    );
  }

  if (error || !store) {
    return (
      <ErrorMessage
        message="매장 정보를 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6">
      <StoreInfo store={store} />
      <MenuList storeId={storeId} />
      <OrderHistory storeId={storeId} />
    </div>
  );
}
