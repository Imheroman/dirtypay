'use client';

import { Card, CardContent } from '@/components/ui/card';
import { formatAmount } from '@/lib/format';
import type { RoundSettlementSummary } from '../types';

interface RoundSettlementCardProps {
  round: RoundSettlementSummary;
  roundTitle?: string;
}

export function RoundSettlementCard({ round, roundTitle }: RoundSettlementCardProps) {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-medium text-foreground">
            {roundTitle || `라운드 ${round.roundId}`}
          </h3>
          <p className="font-bold text-foreground">
            {formatAmount(round.totalAmount)}원
          </p>
        </div>
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>정산 전략: {round.strategy}</span>
        </div>
      </CardContent>
    </Card>
  );
}
