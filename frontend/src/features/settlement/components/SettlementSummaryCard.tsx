'use client';

import { Card, CardContent } from '@/components/ui/card';
import { formatAmount } from '@/lib/format';

interface SettlementSummaryCardProps {
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  roundCount: number;
  memberCount: number;
}

export function SettlementSummaryCard({
  totalAmount,
  paidAmount,
  remainingAmount,
  roundCount,
  memberCount,
}: SettlementSummaryCardProps) {
  const paidPercentage =
    totalAmount > 0 ? Math.round((paidAmount / totalAmount) * 100) : 0;

  return (
    <Card>
      <CardContent className="p-5">
        <div className="text-center mb-4">
          <p className="text-sm text-muted-foreground mb-1">총 정산 금액</p>
          <p className="text-3xl font-bold text-foreground">
            {formatAmount(totalAmount)}원
          </p>
        </div>

        {/* 진행률 바 */}
        <div className="space-y-2 mb-4">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">정산 진행률</span>
            <span className="font-medium text-foreground">{paidPercentage}%</span>
          </div>
          <div
            className="h-2 bg-muted rounded-full overflow-hidden"
            role="progressbar"
            aria-valuenow={paidPercentage}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label="정산 진행률"
          >
            <div
              className="h-full bg-primary transition-all"
              style={{ width: `${paidPercentage}%` }}
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border">
          <div className="text-center">
            <p className="text-sm text-muted-foreground mb-1">정산 완료</p>
            <p className="text-lg font-semibold text-primary">
              {formatAmount(paidAmount)}원
            </p>
          </div>
          <div className="text-center">
            <p className="text-sm text-muted-foreground mb-1">정산 대기</p>
            <p className="text-lg font-semibold text-foreground">
              {formatAmount(remainingAmount)}원
            </p>
          </div>
        </div>

        <div className="flex items-center justify-center gap-4 pt-4 text-sm text-muted-foreground">
          <span>{roundCount}개 라운드</span>
          <span>•</span>
          <span>{memberCount}명 참여</span>
        </div>
      </CardContent>
    </Card>
  );
}
