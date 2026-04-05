'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  TrashIcon,
  UserIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { OrderWithDetails } from '../../types';

interface MyOrderTabProps {
  orders: OrderWithDetails[];
  onDeleteOrder?: (orderId: number) => void;
  isDeleting?: boolean;
  isReadOnly?: boolean;
}

export function MyOrderTab({
  orders,
  onDeleteOrder,
  isDeleting,
  isReadOnly,
}: MyOrderTabProps) {
  const myOrderTotal = orders.reduce(
    (sum, order) => sum + order.totalPrice,
    0
  );

  return (
    <>
      <Card className="mb-4 bg-primary/5 border-primary/20">
        <CardContent className="p-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <UserIcon className="w-5 h-5 text-primary" />
              <span className="font-medium text-foreground">
                내 주문 총액
              </span>
            </div>
            <span className="text-2xl font-bold text-foreground">
              {formatAmount(myOrderTotal)}원
            </span>
          </div>
        </CardContent>
      </Card>

      {orders.length > 0 ? (
        <div className="space-y-2">
          {orders.map((order) => {
            const unitPrice = order.quantity > 0
              ? Math.round(order.totalPrice / order.quantity)
              : 0;

            return (
              <Card key={order.id}>
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium text-foreground">
                        {order.menuName}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {formatAmount(unitPrice)}원 x {order.quantity} ={' '}
                        {formatAmount(order.totalPrice)}원
                      </p>
                    </div>
                    {!isReadOnly && onDeleteOrder && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive hover:text-destructive"
                        onClick={() => onDeleteOrder(order.id)}
                        disabled={isDeleting}
                      >
                        <TrashIcon className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : (
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-muted-foreground">
              아직 주문한 메뉴가 없어요. 메뉴판에서 주문해 보세요.
            </p>
          </CardContent>
        </Card>
      )}
    </>
  );
}
