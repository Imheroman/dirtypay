/** MenuList/MenuSelectSheet 표시용 공통 인터페이스 */
export interface DisplayMenu {
  id: number;
  name: string;
  price: number;
  category?: string | null;
}

/**
 * Menu (메뉴) 타입
 * StoreMenu 기반 — 라운드 메뉴판에서 조회되는 항목
 */
export interface Menu {
  id: number;
  storeId: number;
  name: string;
  price: number;
  category: string | null;
  description: string | null;
  imageUrl: string | null;
  available: boolean;
  sortOrder: number;
  createdDate: string;
}

/**
 * Order (주문) 타입
 * 특정 메뉴에 대한 주문
 */
export interface Order {
  id: number;
  roundId: number;
  menuId: number;
  menuName: string;
  menuPrice: number;
  quantity: number;
  totalPrice: number;
  groupId: number;
  groupName: string;
  createdDate: string;
  updatedDate: string;
}

/**
 * OrderDetail (주문 상세) 타입
 * 주문에 참여한 멤버와 분담 비율
 */
export interface OrderDetail {
  id: number;
  orderId: number;
  orgMemberId: number;
  nickname: string;
  shareRatio: number;
}

/**
 * Order 생성 요청
 */
export interface CreateOrderRequest {
  menuId: number;
  quantity: number;
  memberIds: number[];
  groupId: number;
}

/**
 * Order 수정 요청
 */
export interface UpdateOrderRequest {
  quantity?: number;
}

/**
 * 주문 내역 (상세 포함)
 */
export interface OrderWithDetails extends Order {
  details: OrderDetail[];
}

/**
 * 주문에 참여한 멤버 (버블 UI용)
 */
export interface OrderGroupMember {
  orgMemberId: number;
  nickname: string;
  quantity: number;
  amount: number;
}

/**
 * 개별 주문 기록 (토글 상세용)
 */
export interface OrderRecord {
  orderId: number;
  menuPrice: number;
  quantity: number;
  totalPrice: number;
  createdDate: string;
  members: OrderGroupMember[];
}

/**
 * 메뉴별 주문 그룹 (버블 UI용)
 * - 같은 menuId의 주문이 여러 건이면 orders에 개별 기록 보관
 */
export interface OrderMenuGroup {
  menuId: number;
  menuName: string;
  menuPrice: number;
  quantity: number;
  totalPrice: number;
  members: OrderGroupMember[];
  orders: OrderRecord[];
}

/**
 * 카테고리별 주문 그룹 (버블 UI용)
 */
export interface OrderCategoryGroup {
  category: string;
  totalAmount: number;
  menus: OrderMenuGroup[];
}

/**
 * 개인 주문 (그룹 내 멤버별)
 */
export interface PersonalOrder {
  menuId: number;
  menuName: string;
  price: number;
  quantity: number;
  totalAmount: number;
}

/**
 * 그룹 공유 메뉴
 */
export interface SharedMenu {
  menuId: number;
  menuName: string;
  price: number;
  quantity: number;
}

/**
 * 라운드 그룹 내 멤버
 */
export interface RoundGroupMember {
  orgMemberId: number;
  nickname: string;
  isCurrentUser: boolean;
  personalOrders: PersonalOrder[];
  totalAmount: number;
}

/**
 * 라운드 내 그룹 정보 (그룹 탭 UI용)
 */
export interface RoundGroup {
  groupId: number;
  groupName: string;
  parentGroupId?: number;
  depth: number;
  isParticipating: boolean;
  sharedMenus: SharedMenu[];
  members: RoundGroupMember[];
  childGroups: RoundGroup[];
  totalAmount: number;
}

/**
 * 그룹 생성 요청
 */
export interface CreateGroupRequest {
  name: string;
  parentGroupId?: number;
}

/**
 * 그룹 수정 요청
 */
export interface UpdateGroupRequest {
  name: string;
}

/**
 * 공유 메뉴 저장 요청
 */
export interface SaveSharedMenusRequest {
  menus: { menuId: number; quantity: number }[];
}

/**
 * 그룹 변경 요청
 */
export interface ChangeGroupRequest {
  toGroupId: number;
}

/**
 * 장바구니 항목
 */
export interface CartItem {
  menu: DisplayMenu;
  quantity: number;
}
