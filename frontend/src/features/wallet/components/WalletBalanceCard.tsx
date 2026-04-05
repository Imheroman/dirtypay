'use client';

import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { WalletIcon, PlusIcon, ChevronRightIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { Wallet } from '../types';

interface Props {
  wallet: Wallet;
  onCharge: () => void;
  onTransfer?: () => void;
  compact?: boolean;
}

const statusLabel: Record<string, string> = {
  ACTIVE: '활성',
  FROZEN: '정지',
  CLOSED: '해지',
};

const statusVariant: Record<string, 'default' | 'secondary' | 'destructive'> = {
  ACTIVE: 'default',
  FROZEN: 'destructive',
  CLOSED: 'secondary',
};

export function WalletBalanceCard({ wallet, onCharge, onTransfer, compact }: Props) {
  if (compact) {
    return (
      <Link href="/wallet">
        <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
          <CardContent className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <WalletIcon className="w-5 h-5 text-primary" />
              <div>
                <p className="text-sm text-muted-foreground">내 지갑</p>
                <p className="text-xl font-bold text-foreground">
                  {formatAmount(wallet.balance)}
                  <span className="text-sm font-normal text-muted-foreground ml-1">원</span>
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                onClick={(e) => {
                  e.preventDefault();
                  onCharge();
                }}
              >
                <PlusIcon className="w-4 h-4 mr-1" />
                충전
              </Button>
              <ChevronRightIcon className="w-4 h-4 text-muted-foreground" />
            </div>
          </CardContent>
        </Card>
      </Link>
    );
  }

  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <WalletIcon className="w-5 h-5 text-primary" />
            <span className="text-sm font-medium text-muted-foreground">내 지갑</span>
          </div>
          <Badge variant={statusVariant[wallet.status]}>
            {statusLabel[wallet.status]}
          </Badge>
        </div>

        <p className="text-3xl font-bold text-foreground mb-1">
          {formatAmount(wallet.balance)}
          <span className="text-base font-normal text-muted-foreground ml-1">원</span>
        </p>

        <p className="text-xs text-muted-foreground mb-4">
          오늘 충전한 금액 {formatAmount(wallet.dailyChargedAmount)}원
        </p>

        <div className="flex gap-2">
          <Button className="flex-1" onClick={onCharge}>
            <PlusIcon className="w-4 h-4 mr-1" />
            충전하기
          </Button>
          {onTransfer && (
            <Button variant="outline" className="flex-1" onClick={onTransfer}>
              송금하기
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
