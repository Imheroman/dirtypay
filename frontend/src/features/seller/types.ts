// 판매자 상태
export type SellerStatus = "ACTIVE" | "PENDING_APPROVAL" | "REJECTED";

// 매장 상태
export type StoreStatus = "OPEN" | "TEMPORARILY_CLOSED" | "CLOSED";

// 주문 상태
export type StoreOrderStatus = "PENDING" | "CONFIRMED" | "CANCELLED";

// 판매자
export interface Seller {
  id: number;
  userId: number;
  businessName?: string;
  businessRegistration?: string;
  phone?: string;
  email?: string;
  status: SellerStatus;
  approvedAt?: string;
  createdDate: string;
  updatedDate: string;
}

// 매장
export interface Store {
  id: number;
  sellerId: number;
  name: string;
  address: string;
  description?: string;
  status: StoreStatus;
  menuCount?: number;
  orderCount?: number;
  createdDate: string;
  updatedDate: string;
}

// 메뉴
export interface Menu {
  id: number;
  storeId: number;
  name: string;
  price: number;
  description?: string;
  category?: string;
  imageUrl?: string;
  isAvailable: boolean;
  createdDate: string;
  updatedDate: string;
}

// 주문 아이템
export interface StoreOrderItem {
  menuId: number;
  menuName: string;
  price: number;
  quantity: number;
  priceSnapshot: number;
}

// 주문
export interface StoreOrder {
  id: number;
  storeId: number;
  userId: number;
  userName: string;
  items: StoreOrderItem[];
  totalAmount: number;
  status: StoreOrderStatus;
  orderedAt: string;
  updatedAt: string;
}

// 판매자 등록 요청
export interface BecomeSellerRequest {
  businessName?: string;
  businessRegistration?: string;
  phone?: string;
  email?: string;
}

// 매장 생성 요청
export interface CreateStoreRequest {
  name: string;
  address: string;
  description?: string;
}

// 매장 수정 요청
export interface UpdateStoreRequest {
  name?: string;
  address?: string;
  description?: string;
}

// 매장 상태 변경 요청
export interface ChangeStoreStateRequest {
  status: StoreStatus;
}

// 메뉴 생성 요청
export interface CreateMenuRequest {
  name: string;
  price: number;
  description?: string;
  category?: string;
  imageUrl?: string;
  isAvailable?: boolean;
}

// 메뉴 수정 요청
export interface UpdateMenuRequest {
  name?: string;
  price?: number;
  description?: string;
  category?: string;
  imageUrl?: string;
  isAvailable?: boolean;
}
