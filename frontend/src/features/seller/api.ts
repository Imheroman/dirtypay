import { apiClient } from "@/lib/axios";
import type { ApiResponse } from "@/types/api";
import type {
  Seller,
  Store,
  Menu,
  StoreOrder,
  BecomeSellerRequest,
  CreateStoreRequest,
  UpdateStoreRequest,
  ChangeStoreStateRequest,
  CreateMenuRequest,
  UpdateMenuRequest,
} from "./types";

export const sellerApi = {
  getMySellerInfo: async (): Promise<Seller> => {
    const { data } = await apiClient.get<ApiResponse<Seller>>("/sellers/me");
    return data.data;
  },

  becomeSeller: async (request: BecomeSellerRequest): Promise<Seller> => {
    const { data } = await apiClient.post<ApiResponse<Seller>>(
      "/sellers",
      request,
    );
    return data.data;
  },
};

export const storeApi = {
  getStores: async (sellerId: number): Promise<Store[]> => {
    const { data } = await apiClient.get<ApiResponse<Store[]>>(
      `/sellers/${sellerId}/stores`,
    );
    return data.data;
  },

  getStore: async (storeId: number): Promise<Store> => {
    const { data } = await apiClient.get<ApiResponse<Store>>(
      `/stores/${storeId}`,
    );
    return data.data;
  },

  createStore: async (request: CreateStoreRequest): Promise<Store> => {
    const { data } = await apiClient.post<ApiResponse<Store>>(
      "/stores",
      request,
    );
    return data.data;
  },

  updateStore: async (
    storeId: number,
    request: UpdateStoreRequest,
  ): Promise<Store> => {
    const { data } = await apiClient.put<ApiResponse<Store>>(
      `/stores/${storeId}`,
      request,
    );
    return data.data;
  },

  changeStoreState: async (
    storeId: number,
    request: ChangeStoreStateRequest,
  ): Promise<Store> => {
    const { data } = await apiClient.patch<ApiResponse<Store>>(
      `/stores/${storeId}/state`,
      request,
    );
    return data.data;
  },

  deleteStore: async (storeId: number): Promise<void> => {
    await apiClient.delete(`/stores/${storeId}`);
  },
};

export const storeMenuApi = {
  getMenus: async (storeId: number): Promise<Menu[]> => {
    const { data } = await apiClient.get<ApiResponse<Menu[]>>(
      `/stores/${storeId}/menus`,
    );
    return data.data;
  },

  createMenu: async (
    storeId: number,
    request: CreateMenuRequest,
  ): Promise<Menu> => {
    const { data } = await apiClient.post<ApiResponse<Menu>>(
      `/stores/${storeId}/menus`,
      request,
    );
    return data.data;
  },

  updateMenu: async (
    menuId: number,
    request: UpdateMenuRequest,
  ): Promise<Menu> => {
    const { data } = await apiClient.put<ApiResponse<Menu>>(
      `/menus/${menuId}`,
      request,
    );
    return data.data;
  },

  deleteMenu: async (menuId: number): Promise<void> => {
    await apiClient.delete(`/menus/${menuId}`);
  },
};

export const storeOrderApi = {
  getOrders: async (storeId: number): Promise<StoreOrder[]> => {
    const { data } = await apiClient.get<ApiResponse<StoreOrder[]>>(
      `/stores/${storeId}/orders`,
    );
    return data.data;
  },
};
