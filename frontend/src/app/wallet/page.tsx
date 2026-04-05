'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ChevronLeftIcon } from '@/components/common/Icons';
import {
  useWalletQuery,
  WalletBalanceCard,
  ChargeDialog,
  TransferDialog,
  TransactionList,
} from '@/features/wallet';

export default function WalletPage() {
  const [chargeOpen, setChargeOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);

  const { data: wallet, isLoading } = useWalletQuery();

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href="/"
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">내 지갑</h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg space-y-6">
        {/* 잔액 카드 */}
        {isLoading ? (
          <div className="animate-pulse space-y-4">
            <div className="h-48 bg-muted rounded-lg" />
          </div>
        ) : wallet ? (
          <WalletBalanceCard
            wallet={wallet}
            onCharge={() => setChargeOpen(true)}
            onTransfer={() => setTransferOpen(true)}
          />
        ) : (
          <div className="text-center py-12 text-muted-foreground">
            지갑 정보를 불러올 수 없어요
          </div>
        )}

        {/* 거래 내역 */}
        <div>
          <h2 className="text-base font-semibold mb-3">거래 내역</h2>
          <TransactionList />
        </div>
      </main>

      {/* 충전 다이얼로그 */}
      {wallet && (
        <>
          <ChargeDialog
            open={chargeOpen}
            onOpenChange={setChargeOpen}
            dailyChargedAmount={wallet.dailyChargedAmount}
          />
          <TransferDialog
            open={transferOpen}
            onOpenChange={setTransferOpen}
            balance={wallet.balance}
          />
        </>
      )}
    </div>
  );
}
