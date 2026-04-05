'use client';

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { formatAmount } from '@/lib/format';
import { useMemberSettlementQuery } from '../hooks/useMemberSettlementQuery';
import type { SettlementStrategy } from '../types';

interface SettlementDetailSheetProps {
  isOpen: boolean;
  onClose: () => void;
  sessionId: number;
  orgMemberId: number;
  nickname: string;
  strategy?: SettlementStrategy;
}

export function SettlementDetailSheet({
  isOpen,
  onClose,
  sessionId,
  orgMemberId,
  nickname,
  strategy,
}: SettlementDetailSheetProps) {
  const { data: detail, isLoading } = useMemberSettlementQuery(
    sessionId,
    orgMemberId,
    strategy
  );

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent side="bottom" className="h-[80vh]">
        <SheetHeader>
          <SheetTitle>{nickname}님의 정산 상세</SheetTitle>
          <SheetDescription>
            총 {formatAmount(detail?.totalAmount ?? 0)}원
          </SheetDescription>
        </SheetHeader>

        <div className="py-4 overflow-auto h-[calc(100%-100px)]">
          {isLoading ? (
            <div className="space-y-4">
              <Skeleton className="h-24 w-full rounded-xl" />
              <Skeleton className="h-24 w-full rounded-xl" />
            </div>
          ) : !detail || detail.details.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">상세 내역이 없어요</p>
            </div>
          ) : (
            <div className="space-y-4">
              {detail.details.map((roundDetail) => (
                <Card key={roundDetail.roundId}>
                  <CardContent className="p-4">
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-medium text-foreground">
                        라운드 {roundDetail.roundId}
                      </span>
                      <span className="font-semibold text-foreground">
                        {formatAmount(roundDetail.amount)}원
                      </span>
                    </div>

                    {roundDetail.orders.length > 0 && (
                      <div className="space-y-2 pt-2 border-t border-border">
                        {roundDetail.orders.map((order) => (
                          <div
                            key={order.orderId}
                            className="flex items-center justify-between text-sm"
                          >
                            <div>
                              <span className="text-foreground">
                                {order.menuName}
                              </span>
                              <span className="text-muted-foreground ml-1">
                                x{order.quantity}
                              </span>
                            </div>
                            <span className="text-foreground">
                              {formatAmount(order.myShare)}원
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
