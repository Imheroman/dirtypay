'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { WalletIcon } from '@/components/common/Icons';
import { formatAmount, formatDateTime } from '@/lib/format';
import { cn } from '@/lib/utils';
import { useWalletQuery, ChargeDialog } from '@/features/wallet';
import { useSettlementTransfersQuery } from '../hooks/useSettlementTransfersQuery';
import { useSettlementTransferMutation } from '../hooks/useSettlementTransferMutation';
import { useCancelTransferMutation } from '../hooks/useCancelTransferMutation';
import type { SettlementStrategy, TransferStatus } from '../types';

interface Props {
  sessionId: number;
  orgMemberId: number;
  myAmount: number;
  strategy: SettlementStrategy;
}

const statusConfig: Record<TransferStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  PENDING: { label: '처리 중', variant: 'outline' },
  COMPLETED: { label: '완료', variant: 'default' },
  FAILED: { label: '실패', variant: 'destructive' },
  CANCELLED: { label: '취소됨', variant: 'secondary' },
};

export function SettlementTransferSection({ sessionId, orgMemberId, myAmount, strategy }: Props) {
  const [chargeOpen, setChargeOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data: wallet } = useWalletQuery();
  const { data: transfers } = useSettlementTransfersQuery(sessionId);
  const transferMutation = useSettlementTransferMutation(sessionId);
  const cancelMutation = useCancelTransferMutation(sessionId);

  const isInsufficientBalance = wallet ? wallet.balance < myAmount : false;

  const handleTransfer = () => {
    transferMutation.mutate(
      { orgMemberId, request: { strategyType: strategy } },
      { onSuccess: () => setConfirmOpen(false) }
    );
  };

  return (
    <div className="space-y-4">
      {/* 내 잔액 요약 */}
      {wallet && (
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <WalletIcon className="w-4 h-4 text-primary" />
              <span className="text-sm text-muted-foreground">내 잔액</span>
            </div>
            <span className="text-base font-semibold">{formatAmount(wallet.balance)}원</span>
          </CardContent>
        </Card>
      )}

      {/* 송금 액션 */}
      {myAmount > 0 && (
        <div className="space-y-2">
          {!confirmOpen ? (
            <Button
              className="w-full"
              disabled={transferMutation.isPending}
              onClick={() => setConfirmOpen(true)}
            >
              {formatAmount(myAmount)}원 송금하기
            </Button>
          ) : (
            <Card>
              <CardContent className="p-4 space-y-3">
                <p className="text-sm font-medium">
                  정말 {formatAmount(myAmount)}원을 송금할까요?
                </p>
                {isInsufficientBalance && (
                  <div className="space-y-2">
                    <p className="text-sm text-destructive">잔액이 부족해요</p>
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full"
                      onClick={() => setChargeOpen(true)}
                    >
                      충전하기
                    </Button>
                  </div>
                )}
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => setConfirmOpen(false)}
                  >
                    취소
                  </Button>
                  <Button
                    className="flex-1"
                    disabled={isInsufficientBalance || transferMutation.isPending}
                    onClick={handleTransfer}
                  >
                    {transferMutation.isPending ? '송금 중...' : '송금하기'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* 송금 현황 리스트 */}
      {transfers && transfers.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-sm font-medium text-muted-foreground px-1">송금 현황</h3>
          {transfers.map((transfer) => {
            const config = statusConfig[transfer.status];
            return (
              <Card key={transfer.id}>
                <CardContent className="p-4 flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className={cn('text-sm font-medium')}>
                        {formatAmount(transfer.amount)}원
                      </span>
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {formatDateTime(transfer.createdDate)}
                    </p>
                  </div>
                  {transfer.status === 'PENDING' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={cancelMutation.isPending}
                      onClick={() => cancelMutation.mutate(transfer.id)}
                    >
                      취소
                    </Button>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* 충전 다이얼로그 */}
      {wallet && (
        <ChargeDialog
          open={chargeOpen}
          onOpenChange={setChargeOpen}
          dailyChargedAmount={wallet.dailyChargedAmount}
        />
      )}
    </div>
  );
}
