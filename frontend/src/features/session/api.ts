import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  Session,
  CreateSessionRequest,
  UpdateSessionRequest,
  JoinRequest,
  JoinRequestStatus,
  CreateJoinRequestPayload,
  ApproveJoinRequestPayload,
} from './types';

const BASE_URL = '/sessions';

export const sessionApi = {
  /**
   * 세션 목록 조회
   */
  getSessions: async (): Promise<Session[]> => {
    const { data } = await apiClient.get<ApiResponse<Session[]>>(BASE_URL);
    return data.data;
  },

  /**
   * 세션 상세 조회
   */
  getSession: async (id: number): Promise<Session> => {
    const { data } = await apiClient.get<ApiResponse<Session>>(`${BASE_URL}/${id}`);
    return data.data;
  },

  /**
   * 세션 생성
   */
  createSession: async (request: CreateSessionRequest): Promise<Session> => {
    const { data } = await apiClient.post<ApiResponse<Session>>(BASE_URL, request);
    return data.data;
  },

  /**
   * 세션 수정
   */
  updateSession: async (id: number, request: UpdateSessionRequest): Promise<Session> => {
    const { data } = await apiClient.put<ApiResponse<Session>>(`${BASE_URL}/${id}`, request);
    return data.data;
  },

  /**
   * 만료 세션 목록 조회
   */
  getArchivedSessions: async (): Promise<Session[]> => {
    const { data } = await apiClient.get<ApiResponse<Session[]>>(`${BASE_URL}/archived`);
    return data.data;
  },

  /**
   * 세션 완료 (archive)
   */
  archiveSession: async (id: number): Promise<Session> => {
    const { data } = await apiClient.patch<ApiResponse<Session>>(`${BASE_URL}/${id}/archive`);
    return data.data;
  },

  /**
   * 세션 삭제
   */
  deleteSession: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_URL}/${id}`);
  },
};

export const joinRequestApi = {
  /** 초대 코드로 세션 조회 */
  lookupByInviteCode: async (inviteCode: string): Promise<Session> => {
    const { data } = await apiClient.get<ApiResponse<Session>>(
      `${BASE_URL}/invite/${inviteCode}`
    );
    return data.data;
  },

  /** 참여 요청 제출 */
  create: async (inviteCode: string, payload: CreateJoinRequestPayload): Promise<JoinRequest> => {
    const { data } = await apiClient.post<ApiResponse<JoinRequest>>(
      `${BASE_URL}/invite/${inviteCode}/join-requests`,
      payload
    );
    return data.data;
  },

  /** 참여 요청 목록 조회 */
  list: async (sessionId: number, status?: JoinRequestStatus): Promise<JoinRequest[]> => {
    const params = status ? { status } : {};
    const { data } = await apiClient.get<ApiResponse<JoinRequest[]>>(
      `${BASE_URL}/${sessionId}/join-requests`,
      { params }
    );
    return data.data;
  },

  /** 참여 요청 승인 */
  approve: async (
    sessionId: number,
    requestId: number,
    payload: ApproveJoinRequestPayload
  ): Promise<JoinRequest> => {
    const body = payload.nodeId != null ? payload : undefined;
    const { data } = await apiClient.patch<ApiResponse<JoinRequest>>(
      `${BASE_URL}/${sessionId}/join-requests/${requestId}/approve`,
      body
    );
    return data.data;
  },

  /** 참여 요청 거절 */
  reject: async (sessionId: number, requestId: number): Promise<JoinRequest> => {
    const { data } = await apiClient.patch<ApiResponse<JoinRequest>>(
      `${BASE_URL}/${sessionId}/join-requests/${requestId}/reject`
    );
    return data.data;
  },
};
