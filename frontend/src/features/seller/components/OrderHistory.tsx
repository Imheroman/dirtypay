"use client";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { EmptyList } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Skeleton } from "@/components/ui/skeleton";
import { formatAmount, formatDateTime } from "@/lib/format";
import { useOrderHistoryQuery } from "../hooks";
import type { StoreOrderStatus } from "../types";

const orderStatusConfig: Record<
  StoreOrderStatus,
  { label: string; variant: "default" | "secondary" | "outline" }
> = {
  PENDING: { label: "대기", variant: "secondary" },
  CONFIRMED: { label: "확인", variant: "default" },
  CANCELLED: { label: "취소", variant: "outline" },
};

interface OrderHistoryProps {
  storeId: number;
}

export function OrderHistory({ storeId }: OrderHistoryProps) {
  const {
    data: orders,
    isLoading,
    error,
    refetch,
  } = useOrderHistoryQuery({ storeId });

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-[96px] rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <ErrorMessage
        message="주문 내역을 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div>
      <h3 className="font-semibold text-foreground mb-4">주문 내역</h3>
      {!orders?.length ? (
        <EmptyList message="아직 주문 내역이 없어요" />
      ) : (
        <div className="space-y-2">
          {orders.map((order) => {
            const config = orderStatusConfig[order.status];
            return (
              <Card key={order.id}>
                <CardContent className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-foreground">
                          {order.userName}
                        </span>
                        <Badge variant={config.variant} className="text-xs">
                          {config.label}
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {formatDateTime(order.orderedAt)}
                      </p>
                    </div>
                    <span className="font-semibold text-foreground">
                      {formatAmount(order.totalAmount)}원
                    </span>
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {order.items
                      .map((item) => `${item.menuName} x${item.quantity}`)
                      .join(", ")}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
