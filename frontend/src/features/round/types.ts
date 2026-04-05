/**
 * Round (라운드) 타입
 * 한 세션 내에서 개별 식사/모임을 나타냄
 */
export interface Round {
  id: number;
  sessionId: number;
  title: string;
  place?: string;
  roundDate?: string;
  status: RoundStatus;
  sortOrder: number;
  totalAmount?: number;
  participantCount?: number;
  storeId?: number;
  createdDate: string;
  updatedDate: string;
}

export type RoundStatus = 'OPEN' | 'CLOSED';

/**
 * Round 생성 요청
 */
export interface CreateRoundRequest {
  title: string;
  place?: string;
  roundDate?: string;
  sortOrder?: number;
  storeId?: number;
}

/**
 * Round 수정 요청
 */
export interface UpdateRoundRequest {
  title: string;
  place?: string;
  roundDate?: string;
  sortOrder?: number;
  storeId?: number;
}

/**
 * RoundParticipant (라운드 참여자) 타입
 */
export interface RoundParticipant {
  id: number;
  roundId: number;
  orgMemberId: number;
  nickname: string;
  isExcluded: boolean;
}
