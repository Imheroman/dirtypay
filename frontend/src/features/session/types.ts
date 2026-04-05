export interface Session {
  id: number;
  title: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  status: SessionStatus;
  ownerId?: number;
  memberCount?: number;
  roundCount?: number;
  totalAmount?: number;
  inviteCode?: string;
  createdDate: string;
  updatedDate: string;
}

export type SessionStatus = 'ACTIVE' | 'ARCHIVED';

export interface CreateSessionRequest {
  title: string;
  description?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdateSessionRequest {
  title?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
}

// --- Join Request ---

export type JoinRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface JoinRequest {
  id: number;
  sessionId: number;
  requesterId: number;
  nickname: string;
  message: string | null;
  status: JoinRequestStatus;
  createdDate: string;
  updatedDate: string;
}

export interface CreateJoinRequestPayload {
  nickname: string;
  message?: string;
}

export interface ApproveJoinRequestPayload {
  nodeId?: number;
}
