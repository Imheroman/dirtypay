// Enums
export type StoreType = 'DIRECT' | 'POS_INTEGRATED' | 'CUSTOM';
export type StoreStatus = 'ACTIVE' | 'INACTIVE' | 'CLOSED';

// Pagination Response
export interface StorePaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

// Store
export interface Store {
  id: number;
  ownerId: number;
  name: string;
  businessNumber: string | null;
  address: string;
  phone: string | null;
  description: string | null;
  storeType: StoreType;
  status: StoreStatus;
  isPublic: boolean;
  createdDate: string;
  updatedDate: string;
}

export interface CreateStoreRequest {
  name: string;
  address: string;
  description?: string;
  storeType: StoreType;
  businessNumber?: string;
  phone?: string;
  isPublic?: boolean;
  posIntegrationKey?: string;
}

export interface UpdateStoreRequest {
  name?: string;
  address?: string;
  description?: string;
  phone?: string;
}

export interface ChangeStoreStatusRequest {
  status: StoreStatus;
}

// StoreMenu
export interface StoreMenu {
  id: number;
  storeId: number;
  name: string;
  description: string | null;
  price: number;
  category: string | null;
  imageUrl: string | null;
  available: boolean;
  sortOrder: number;
  createdDate: string;
}

export interface CreateStoreMenuRequest {
  name: string;
  description?: string;
  price: number;
  category?: string;
  imageUrl?: string;
  available: boolean;
  sortOrder?: number;
}

export interface UpdateStoreMenuRequest {
  name?: string;
  description?: string;
  price?: number;
  category?: string;
  imageUrl?: string;
  sortOrder?: number;
}

// Order Types
export type StoreOrderStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface StoreOrder {
  id: number;
  storeId: number;
  menuId: number;
  quantity: number;
  totalPrice: number;
  status: StoreOrderStatus;
  orderNumber: string;
  customerName: string | null;
  customerPhone: string | null;
  memberId: number | null;
  unitPrice: number;
  menuName: string;
  createdDate: string;
}

export interface StoreOrderPageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface CreateStoreOrderRequest {
  menuId: number;
  quantity: number;
  memberId?: number;
  customerName?: string;
  customerPhone?: string;
}

export interface ChangeStoreOrderStatusRequest {
  status: StoreOrderStatus;
}

// Review Types
export interface StoreReview {
  id: number;
  storeId: number;
  memberId: number;
  rating: number;
  content: string | null;
  createdDate: string;
}

export interface CreateStoreReviewRequest {
  rating: number;
  content?: string;
}

export interface UpdateStoreReviewRequest {
  rating: number;
  content?: string;
}

// Statistics
export interface StoreStatistics {
  storeId: number;
  averageDailyOrders: number;
  totalOrders: number;
  totalRevenue: number;
  periodStart: string;
  periodEnd: string;
}

export interface StorePopularMenusResponse {
  menus: StorePopularMenu[];
}

export interface StorePopularMenu {
  menuId: number;
  menuName: string;
  orderCount: number;
  revenue: number;
}
