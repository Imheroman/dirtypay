'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { formatAmount } from '@/lib/format';
import { useTransferMutation } from '../hooks';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  balance: number;
  receiverEmail?: string;
  receiverName?: string;
}

export function TransferDialog({
  open,
  onOpenChange,
  balance,
  receiverEmail: initialEmail,
  receiverName,
}: Props) {
  const [amount, setAmount] = useState('');
  const [email, setEmail] = useState(initialEmail ?? '');
  const transferMutation = useTransferMutation();

  const numAmount = Number(amount) || 0;
  const isOverBalance = numAmount > balance;
  const isValid = numAmount > 0 && email.trim().length > 0 && !isOverBalance;

  const handleSubmit = () => {
    if (!isValid) return;
    transferMutation.mutate(
      {
        receiverEmail: email.trim(),
        amount: numAmount,
        idempotencyKey: crypto.randomUUID(),
      },
      {
        onSuccess: () => {
          setAmount('');
          setEmail('');
          onOpenChange(false);
        },
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>송금하기</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {receiverName ? (
            <p className="text-sm text-muted-foreground">
              받는 사람: <span className="font-medium text-foreground">{receiverName}</span>
            </p>
          ) : (
            <Input
              type="email"
              placeholder="받는 사람 이메일"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          )}

          <div>
            <Input
              type="number"
              placeholder="송금 금액"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min={0}
            />
            <p className="text-xs text-muted-foreground mt-1.5">
              잔액: {formatAmount(balance)}원
            </p>
            {isOverBalance && (
              <p className="text-xs text-destructive mt-1">
                잔액이 부족해요
              </p>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button
            className="w-full"
            disabled={!isValid || transferMutation.isPending}
            onClick={handleSubmit}
          >
            {transferMutation.isPending ? '송금 중...' : `${formatAmount(numAmount)}원 송금하기`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
