import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  Order,
  OrderWithDetails,
  CreateOrderRequest,
  UpdateOrderRequest,
  RoundGroup,
  CreateGroupRequest,
  UpdateGroupRequest,
  SaveSharedMenusRequest,
  ChangeGroupRequest,
} from './types';

export const orderApi = {
  // ============ Order API ============

  /**
   * 라운드의 주문 목록 조회
   */
  getOrders: async (roundId: number, groupId?: number): Promise<OrderWithDetails[]> => {
    const params = groupId ? { groupId } : undefined;
    const { data } = await apiClient.get<ApiResponse<OrderWithDetails[]>>(
      `/rounds/${roundId}/orders`,
      { params }
    );
    return data.data;
  },

  /**
   * 주문 생성
   */
  createOrder: async (roundId: number, request: CreateOrderRequest): Promise<Order> => {
    const { data } = await apiClient.post<ApiResponse<Order>>(
      `/rounds/${roundId}/orders`,
      request
    );
    return data.data;
  },

  /**
   * 주문 수정
   */
  updateOrder: async (orderId: number, request: UpdateOrderRequest): Promise<Order> => {
    const { data } = await apiClient.put<ApiResponse<Order>>(
      `/orders/${orderId}`,
      request
    );
    return data.data;
  },

  /**
   * 주문 삭제
   */
  deleteOrder: async (orderId: number): Promise<void> => {
    await apiClient.delete(`/orders/${orderId}`);
  },
};

export const groupApi = {
  /**
   * 라운드의 그룹 목록 조회 (계층 구조)
   */
  getGroups: async (roundId: number): Promise<RoundGroup[]> => {
    const { data } = await apiClient.get<ApiResponse<RoundGroup[]>>(
      `/rounds/${roundId}/groups`
    );
    return data.data;
  },

  /**
   * 그룹 생성
   */
  createGroup: async (roundId: number, request: CreateGroupRequest): Promise<RoundGroup> => {
    const { data } = await apiClient.post<ApiResponse<RoundGroup>>(
      `/rounds/${roundId}/groups`,
      request
    );
    return data.data;
  },

  /**
   * 그룹 수정
   */
  updateGroup: async (groupId: number, request: UpdateGroupRequest): Promise<RoundGroup> => {
    const { data } = await apiClient.put<ApiResponse<RoundGroup>>(
      `/groups/${groupId}`,
      request
    );
    return data.data;
  },

  /**
   * 그룹 삭제
   */
  deleteGroup: async (groupId: number): Promise<void> => {
    await apiClient.delete(`/groups/${groupId}`);
  },

  /**
   * 그룹 참여
   */
  joinGroup: async (groupId: number): Promise<void> => {
    await apiClient.post(`/groups/${groupId}/join`);
  },

  /**
   * 그룹 탈퇴
   */
  leaveGroup: async (groupId: number): Promise<void> => {
    await apiClient.delete(`/groups/${groupId}/leave`);
  },

  /**
   * 그룹 변경 (단일 트랜잭션)
   */
  changeGroup: async (groupId: number, request: ChangeGroupRequest): Promise<void> => {
    await apiClient.put(`/groups/${groupId}/change`, request);
  },

  /**
   * 공유 메뉴 저장 (전체 교체)
   */
  saveSharedMenus: async (groupId: number, request: SaveSharedMenusRequest): Promise<void> => {
    await apiClient.put(`/groups/${groupId}/shared-menus`, request);
  },
};
