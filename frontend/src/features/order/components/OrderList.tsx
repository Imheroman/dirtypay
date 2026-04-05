'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EditIcon, TrashIcon, UsersIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { OrderWithDetails } from '../types';

interface OrderListProps {
  orders: OrderWithDetails[];
  onEditOrder?: (order: OrderWithDetails) => void;
  onDeleteOrder?: (orderId: number) => void;
  isEditable?: boolean;
}

export function OrderList({
  orders,
  onEditOrder,
  onDeleteOrder,
  isEditable = true,
}: OrderListProps) {
  if (orders.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-muted-foreground">등록된 주문이 없어요</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      {orders.map((order) => (
        <Card key={order.id}>
          <CardContent className="p-4">
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <p className="font-medium text-foreground truncate">
                    {order.menuName}
                  </p>
                  <Badge variant="secondary" className="text-xs shrink-0">
                    x{order.quantity}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground">
                  <span className="font-medium text-foreground">
                    {formatAmount(order.totalPrice)}원
                  </span>
                  {order.quantity > 1 && ` (${order.quantity}개)`}
                </p>
              </div>
              {isEditable && (
                <div className="flex items-center gap-1 shrink-0">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-11 w-11"
                    onClick={() => onEditOrder?.(order)}
                    aria-label={`${order.menuName} 수정`}
                  >
                    <EditIcon className="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-11 w-11 text-destructive hover:text-destructive"
                    onClick={() => onDeleteOrder?.(order.id)}
                    aria-label={`${order.menuName} 삭제`}
                  >
                    <TrashIcon className="w-4 h-4" />
                  </Button>
                </div>
              )}
            </div>

            {/* 참여자 목록 */}
            <div className="flex items-start gap-2 pt-3 border-t border-border">
              <UsersIcon className="w-4 h-4 text-muted-foreground mt-0.5 shrink-0" />
              <div className="flex flex-wrap gap-1.5">
                {order.details.map((detail) => (
                  <Badge
                    key={detail.id}
                    variant="outline"
                    className="text-xs font-normal"
                  >
                    {detail.nickname}
                    {detail.shareRatio !== 1 && (
                      <span className="ml-1 opacity-70">
                        ×{detail.shareRatio}
                      </span>
                    )}
                  </Badge>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
