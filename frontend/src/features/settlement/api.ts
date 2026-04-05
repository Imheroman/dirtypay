import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  SessionSettlement,
  RoundSettlement,
  MemberSettlementDetail,
  SettlementStrategy,
  GroupOrdersResponse,
  GroupSettlement,
  SettlementTransfer,
  CreateTransferRequest,
} from './types';

export const settlementApi = {
  /**
   * 세션 전체 정산 결과 조회
   */
  getSessionSettlement: async (
    sessionId: number,
    strategy?: SettlementStrategy
  ): Promise<SessionSettlement> => {
    const params = strategy ? { strategy } : undefined;
    const { data } = await apiClient.get<ApiResponse<SessionSettlement>>(
      `/sessions/${sessionId}/settlement`,
      { params }
    );
    return data.data;
  },

  /**
   * 라운드별 정산 결과 조회
   */
  getRoundSettlement: async (
    roundId: number,
    strategy?: SettlementStrategy
  ): Promise<RoundSettlement> => {
    const params = strategy ? { strategy } : undefined;
    const { data } = await apiClient.get<ApiResponse<RoundSettlement>>(
      `/rounds/${roundId}/settlement`,
      { params }
    );
    return data.data;
  },

  /**
   * 멤버 상세 정산 조회
   */
  getMemberSettlement: async (
    sessionId: number,
    orgMemberId: number,
    strategy?: SettlementStrategy
  ): Promise<MemberSettlementDetail> => {
    const params = strategy ? { strategy } : undefined;
    const { data } = await apiClient.get<ApiResponse<MemberSettlementDetail>>(
      `/sessions/${sessionId}/settlement/members/${orgMemberId}`,
      { params }
    );
    return data.data;
  },

  /**
   * 그룹별 주문 내역 조회
   */
  getGroupOrders: async (
    roundId: number,
    groupId: number
  ): Promise<GroupOrdersResponse> => {
    const { data } = await apiClient.get<ApiResponse<GroupOrdersResponse>>(
      `/rounds/${roundId}/settlement/groups/${groupId}`
    );
    return data.data;
  },

  /**
   * 그룹별 정산 금액 조회
   */
  getGroupSettlement: async (
    roundId: number,
    groupId: number,
    strategy?: SettlementStrategy
  ): Promise<GroupSettlement> => {
    const params = strategy ? { strategy } : undefined;
    const { data } = await apiClient.get<ApiResponse<GroupSettlement>>(
      `/rounds/${roundId}/settlement/groups/${groupId}/amounts`,
      { params }
    );
    return data.data;
  },

  /**
   * 정산 송금 생성
   */
  createTransfer: async (
    sessionId: number,
    orgMemberId: number,
    request: CreateTransferRequest
  ): Promise<SettlementTransfer> => {
    const { data } = await apiClient.post<ApiResponse<SettlementTransfer>>(
      `/settlements/${sessionId}/transfers`,
      request,
      { params: { orgMemberId } }
    );
    return data.data;
  },

  /**
   * 정산 송금 현황 조회
   */
  getTransfers: async (sessionId: number): Promise<SettlementTransfer[]> => {
    const { data } = await apiClient.get<ApiResponse<SettlementTransfer[]>>(
      `/settlements/${sessionId}/transfers`
    );
    return data.data;
  },

  /**
   * 정산 송금 취소
   */
  cancelTransfer: async (transferId: number): Promise<void> => {
    await apiClient.post(`/settlements/transfers/${transferId}/cancel`);
  },

  /**
   * 멤버 정산 금액 업데이트 (납부 처리)
   */
  updateMemberPayment: async (
    sessionId: number,
    orgMemberId: number,
    paidAmount: number,
    strategy?: SettlementStrategy
  ): Promise<MemberSettlementDetail> => {
    const params = strategy ? { strategy } : undefined;
    const { data } = await apiClient.put<ApiResponse<MemberSettlementDetail>>(
      `/sessions/${sessionId}/settlement/members/${orgMemberId}`,
      { paidAmount },
      { params }
    );
    return data.data;
  },
};
