import { apiClient } from '@/lib/axios';
import type { ApiResponse, PaginationParams } from '@/types/api';
import type {
  Store,
  CreateStoreRequest,
  UpdateStoreRequest,
  ChangeStoreStatusRequest,
  StorePaginatedResponse,
  StoreMenu,
  CreateStoreMenuRequest,
  UpdateStoreMenuRequest,
  StoreStatistics,
  StorePopularMenusResponse,
  StoreOrder,
  StoreOrderPageResponse,
  CreateStoreOrderRequest,
  ChangeStoreOrderStatusRequest,
  StoreReview,
  CreateStoreReviewRequest,
  UpdateStoreReviewRequest,
} from './types';

export const storeApi = {
  /**
   * 매장 등록
   */
  createStore: async (request: CreateStoreRequest): Promise<Store> => {
    const { data } = await apiClient.post<ApiResponse<Store>>('/stores', request);
    return data.data;
  },

  /**
   * 매장 목록 조회 (페이지네이션)
   */
  getStores: async (params?: PaginationParams & { scope?: string }): Promise<StorePaginatedResponse<Store>> => {
    const { data } = await apiClient.get<ApiResponse<StorePaginatedResponse<Store>>>('/stores', { params });
    return data.data;
  },

  /**
   * 매장 상세 조회
   */
  getStore: async (storeId: number): Promise<Store> => {
    const { data } = await apiClient.get<ApiResponse<Store>>(`/stores/${storeId}`);
    return data.data;
  },

  /**
   * 매장 정보 수정
   */
  updateStore: async (storeId: number, request: UpdateStoreRequest): Promise<Store> => {
    const { data } = await apiClient.put<ApiResponse<Store>>(`/stores/${storeId}`, request);
    return data.data;
  },

  /**
   * 매장 상태 변경
   */
  changeStoreStatus: async (storeId: number, request: ChangeStoreStatusRequest): Promise<Store> => {
    const { data } = await apiClient.patch<ApiResponse<Store>>(`/stores/${storeId}/status`, request);
    return data.data;
  },

  /**
   * 매장 삭제 (소프트 삭제)
   */
  deleteStore: async (storeId: number): Promise<void> => {
    await apiClient.delete(`/stores/${storeId}`);
  },

  /**
   * 매장 통계 조회
   */
  getStatistics: async (storeId: number, params?: Record<string, unknown>): Promise<StoreStatistics> => {
    const { data } = await apiClient.get<ApiResponse<StoreStatistics>>(`/stores/${storeId}/statistics`, { params });
    return data.data;
  },

  /**
   * 인기 메뉴 조회
   */
  getPopularMenus: async (storeId: number, params?: Record<string, unknown>): Promise<StorePopularMenusResponse> => {
    const { data } = await apiClient.get<ApiResponse<StorePopularMenusResponse>>(
      `/stores/${storeId}/statistics/popular-menus`,
      { params }
    );
    return data.data;
  },
};

export const storeMenuApi = {
  /**
   * 메뉴 등록
   */
  createMenu: async (storeId: number, request: CreateStoreMenuRequest): Promise<StoreMenu> => {
    const { data } = await apiClient.post<ApiResponse<StoreMenu>>(`/stores/${storeId}/menus`, request);
    return data.data;
  },

  /**
   * 메뉴 목록 조회
   */
  getMenus: async (storeId: number): Promise<StoreMenu[]> => {
    const { data } = await apiClient.get<ApiResponse<StoreMenu[]>>(`/stores/${storeId}/menus`);
    return data.data;
  },

  /**
   * 판매 가능한 메뉴 조회
   */
  getAvailableMenus: async (storeId: number): Promise<StoreMenu[]> => {
    const { data } = await apiClient.get<ApiResponse<StoreMenu[]>>(`/stores/${storeId}/menus/available`);
    return data.data;
  },

  /**
   * 메뉴 상세 조회
   */
  getMenu: async (storeId: number, menuId: number): Promise<StoreMenu> => {
    const { data } = await apiClient.get<ApiResponse<StoreMenu>>(`/stores/${storeId}/menus/${menuId}`);
    return data.data;
  },

  /**
   * 메뉴 수정
   */
  updateMenu: async (storeId: number, menuId: number, request: UpdateStoreMenuRequest): Promise<StoreMenu> => {
    const { data } = await apiClient.put<ApiResponse<StoreMenu>>(
      `/stores/${storeId}/menus/${menuId}`,
      request
    );
    return data.data;
  },

  /**
   * 메뉴 판매 상태 토글
   */
  toggleMenuAvailability: async (storeId: number, menuId: number): Promise<StoreMenu> => {
    const { data } = await apiClient.patch<ApiResponse<StoreMenu>>(
      `/stores/${storeId}/menus/${menuId}/toggle`
    );
    return data.data;
  },

  /**
   * 메뉴 삭제 (소프트 삭제)
   */
  deleteMenu: async (storeId: number, menuId: number): Promise<void> => {
    await apiClient.delete(`/stores/${storeId}/menus/${menuId}`);
  },
};

export const storeOrderApi = {
  /**
   * 주문 생성
   */
  createOrder: async (storeId: number, request: CreateStoreOrderRequest): Promise<StoreOrder> => {
    const { data } = await apiClient.post<ApiResponse<StoreOrder>>(`/stores/${storeId}/orders`, request);
    return data.data;
  },

  /**
   * 주문 목록 조회 (페이지네이션)
   */
  getOrders: async (storeId: number, params?: Record<string, unknown>): Promise<StoreOrderPageResponse<StoreOrder>> => {
    const { data } = await apiClient.get<ApiResponse<StoreOrderPageResponse<StoreOrder>>>(
      `/stores/${storeId}/orders`,
      { params }
    );
    return data.data;
  },

  /**
   * 주문 상세 조회
   */
  getOrder: async (storeId: number, orderId: number): Promise<StoreOrder> => {
    const { data } = await apiClient.get<ApiResponse<StoreOrder>>(`/stores/${storeId}/orders/${orderId}`);
    return data.data;
  },

  /**
   * 주문 상태 변경
   */
  changeStatus: async (storeId: number, orderId: number, request: ChangeStoreOrderStatusRequest): Promise<StoreOrder> => {
    const { data } = await apiClient.patch<ApiResponse<StoreOrder>>(
      `/stores/${storeId}/orders/${orderId}/status`,
      request
    );
    return data.data;
  },

  /**
   * 주문 취소
   */
  cancelOrder: async (storeId: number, orderId: number): Promise<void> => {
    await apiClient.delete(`/stores/${storeId}/orders/${orderId}`);
  },
};

export const storeReviewApi = {
  /**
   * 리뷰 작성
   */
  createReview: async (storeId: number, request: CreateStoreReviewRequest): Promise<StoreReview> => {
    const { data } = await apiClient.post<ApiResponse<StoreReview>>(`/stores/${storeId}/reviews`, request);
    return data.data;
  },

  /**
   * 리뷰 목록 조회
   */
  getReviews: async (storeId: number): Promise<StoreReview[]> => {
    const { data } = await apiClient.get<ApiResponse<StoreReview[]>>(`/stores/${storeId}/reviews`);
    return data.data;
  },

  /**
   * 리뷰 상세 조회
   */
  getReview: async (storeId: number, reviewId: number): Promise<StoreReview> => {
    const { data } = await apiClient.get<ApiResponse<StoreReview>>(`/stores/${storeId}/reviews/${reviewId}`);
    return data.data;
  },

  /**
   * 리뷰 수정
   */
  updateReview: async (storeId: number, reviewId: number, request: UpdateStoreReviewRequest): Promise<StoreReview> => {
    const { data } = await apiClient.put<ApiResponse<StoreReview>>(
      `/stores/${storeId}/reviews/${reviewId}`,
      request
    );
    return data.data;
  },

  /**
   * 리뷰 삭제
   */
  deleteReview: async (storeId: number, reviewId: number): Promise<void> => {
    await apiClient.delete(`/stores/${storeId}/reviews/${reviewId}`);
  },

  /**
   * 평균 별점 조회
   */
  getAverageRating: async (storeId: number): Promise<number> => {
    const { data } = await apiClient.get<ApiResponse<number>>(`/stores/${storeId}/reviews/rating`);
    return data.data;
  },
};
