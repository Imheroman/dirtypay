/**
 * 정산 전략 타입
 */
export type SettlementStrategy = 'OWNER' | 'RANDOM' | 'ROUND_UP';

/**
 * Session 전체 정산 결과 (백엔드 SessionSettlementResponse)
 */
export interface SessionSettlement {
  sessionId: number;
  totalAmount: number;
  strategy: SettlementStrategy;
  settlements: MemberAmount[];
  rounds: RoundSettlementSummary[];
}

/**
 * 멤버별 정산 금액 (백엔드 MemberAmountResponse)
 */
export interface MemberAmount {
  orgMemberId: number;
  nickname: string;
  amount: number;
  isExcluded: boolean;
  isPaid: boolean;
  paidAmount: number;
  remainingAmount: number;
}

/**
 * 라운드별 정산 요약 (세션 정산 내 사용)
 */
export interface RoundSettlementSummary {
  roundId: number;
  totalAmount: number;
  strategy: SettlementStrategy;
  settlements: MemberAmount[];
}

/**
 * Round 정산 결과 (백엔드 RoundSettlementResponse)
 */
export interface RoundSettlement {
  roundId: number;
  totalAmount: number;
  strategy: SettlementStrategy;
  settlements: MemberAmount[];
}

/**
 * 멤버 상세 정산 (백엔드 MemberSettlementResponse)
 */
export interface MemberSettlementDetail {
  orgMemberId: number;
  totalAmount: number;
  isPaid: boolean;
  paidAmount: number;
  remainingAmount: number;
  details: MemberRoundDetail[];
}

/**
 * 멤버의 라운드별 정산 상세
 */
export interface MemberRoundDetail {
  roundId: number;
  amount: number;
  orders: MemberOrderDetail[];
}

/**
 * 멤버의 주문별 정산 상세
 */
export interface MemberOrderDetail {
  orderId: number;
  menuName: string;
  quantity: number;
  totalPrice: number;
  myShare: number;
}

// ============ Group(그룹)별 정산 ============

/**
 * 그룹별 주문 내역 응답
 */
export interface GroupOrdersResponse {
  groupId: number;
  groupName: string;
  totalAmount: number;
  categories: GroupCategoryGroup[];
}

/**
 * 그룹별 카테고리 그룹
 */
export interface GroupCategoryGroup {
  category: string;
  totalAmount: number;
  menus: GroupMenuSummary[];
}

/**
 * 그룹별 메뉴 요약
 */
export interface GroupMenuSummary {
  menuId: number;
  menuName: string;
  totalPrice: number;
  orderCount: number;
  members: GroupMemberCount[];
  orders: GroupOrderHistory[];
}

/**
 * 그룹 멤버별 주문 수량
 */
export interface GroupMemberCount {
  orgMemberId: number;
  nickname: string;
  count: number;
}

/**
 * 그룹 주문 기록
 */
export interface GroupOrderHistory {
  orderId: number;
  menuPrice: number;
  quantity: number;
  totalPrice: number;
  memberNicknames: string[];
  createdDate: string;
}

/**
 * 그룹별 정산 결과
 */
export interface GroupSettlement {
  groupId: number;
  groupName: string;
  totalAmount: number;
  strategy: SettlementStrategy;
  settlements: MemberAmount[];
}

// ============ Settlement Transfer ============

export type TransferStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface SettlementTransfer {
  id: number;
  sessionId: number;
  orgMemberId: number;
  senderWalletId: number;
  receiverWalletId: number;
  amount: number;
  status: TransferStatus;
  createdDate: string;
}

export interface CreateTransferRequest {
  strategyType: SettlementStrategy;
}
