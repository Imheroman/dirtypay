export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type TransactionType = 'CHARGE' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'REFUND';
export type TransactionStatus = 'COMPLETED' | 'FAILED';

export interface Wallet {
  id: number;
  memberId: number;
  balance: number;
  dailyChargedAmount: number;
  status: WalletStatus;
  createdDate: string;
}

export interface WalletTransaction {
  id: number;
  walletId: number;
  type: TransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  counterpartyWalletId?: number;
  referenceType?: string;
  referenceId?: number;
  description?: string;
  status: TransactionStatus;
  createdDate: string;
}

export interface ChargeRequest {
  amount: number;
}

export interface TransferRequest {
  receiverEmail: string;
  amount: number;
  idempotencyKey?: string;
  description?: string;
}
