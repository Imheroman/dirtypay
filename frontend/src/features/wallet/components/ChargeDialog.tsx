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
import { useChargeMutation } from '../hooks';

const DAILY_LIMIT = 3_000_000;
const PRESETS = [10_000, 30_000, 50_000];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dailyChargedAmount: number;
}

export function ChargeDialog({ open, onOpenChange, dailyChargedAmount }: Props) {
  const [amount, setAmount] = useState('');
  const chargeMutation = useChargeMutation();
  const remaining = DAILY_LIMIT - dailyChargedAmount;

  const handlePreset = (value: number) => {
    setAmount(String(value));
  };

  const handleSubmit = () => {
    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) return;
    chargeMutation.mutate(
      { amount: numAmount },
      {
        onSuccess: () => {
          setAmount('');
          onOpenChange(false);
        },
      }
    );
  };

  const numAmount = Number(amount) || 0;
  const isOverLimit = numAmount > remaining;
  const isValid = numAmount > 0 && !isOverLimit;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>충전하기</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="flex gap-2">
            {PRESETS.map((preset) => (
              <Button
                key={preset}
                variant="outline"
                size="sm"
                className="flex-1"
                onClick={() => handlePreset(preset)}
              >
                {formatAmount(preset)}원
              </Button>
            ))}
          </div>

          <div>
            <Input
              type="number"
              placeholder="직접 입력"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min={0}
            />
            <p className="text-xs text-muted-foreground mt-1.5">
              일일 충전 한도 잔여: {formatAmount(remaining)}원
            </p>
            {isOverLimit && (
              <p className="text-xs text-destructive mt-1">
                일일 충전 한도를 초과해요
              </p>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button
            className="w-full"
            disabled={!isValid || chargeMutation.isPending}
            onClick={handleSubmit}
          >
            {chargeMutation.isPending ? '충전 중...' : `${formatAmount(numAmount)}원 충전하기`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
