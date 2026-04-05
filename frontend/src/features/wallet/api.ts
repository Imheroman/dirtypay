import { apiClient } from '@/lib/axios';
import type { ApiResponse, PaginationParams, PaginatedResponse } from '@/types/api';
import type { Wallet, WalletTransaction, ChargeRequest, TransferRequest } from './types';

export const walletApi = {
  getMyWallet: async (): Promise<Wallet> => {
    const { data } = await apiClient.get<ApiResponse<Wallet>>('/wallets/me');
    return data.data;
  },

  getTransactions: async (
    params?: PaginationParams
  ): Promise<PaginatedResponse<WalletTransaction>> => {
    const { data } = await apiClient.get<ApiResponse<PaginatedResponse<WalletTransaction>>>(
      '/wallets/me/transactions',
      { params }
    );
    return data.data;
  },

  charge: async (request: ChargeRequest): Promise<Wallet> => {
    const { data } = await apiClient.post<ApiResponse<Wallet>>('/wallets/charge', request);
    return data.data;
  },

  transfer: async (request: TransferRequest): Promise<WalletTransaction> => {
    const { data } = await apiClient.post<ApiResponse<WalletTransaction>>(
      '/wallets/transfer',
      request
    );
    return data.data;
  },
};
