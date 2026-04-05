import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { Round, CreateRoundRequest, UpdateRoundRequest, RoundParticipant } from './types';

export const roundApi = {
  /**
   * 세션의 라운드 목록 조회
   */
  getRounds: async (sessionId: number): Promise<Round[]> => {
    const { data } = await apiClient.get<ApiResponse<Round[]>>(
      `/sessions/${sessionId}/rounds`
    );
    return data.data;
  },

  /**
   * 라운드 상세 조회
   */
  getRound: async (roundId: number): Promise<Round> => {
    const { data } = await apiClient.get<ApiResponse<Round>>(
      `/rounds/${roundId}`
    );
    return data.data;
  },

  /**
   * 라운드 생성
   */
  createRound: async (sessionId: number, request: CreateRoundRequest): Promise<Round> => {
    const { data } = await apiClient.post<ApiResponse<Round>>(
      `/sessions/${sessionId}/rounds`,
      request
    );
    return data.data;
  },

  /**
   * 라운드 수정
   */
  updateRound: async (roundId: number, request: UpdateRoundRequest): Promise<Round> => {
    const { data } = await apiClient.put<ApiResponse<Round>>(
      `/rounds/${roundId}`,
      request
    );
    return data.data;
  },

  /**
   * 라운드 삭제
   */
  deleteRound: async (roundId: number): Promise<void> => {
    await apiClient.delete(`/rounds/${roundId}`);
  },

  /**
   * 라운드 상태 변경
   */
  updateRoundStatus: async (roundId: number, status: 'OPEN' | 'CLOSED'): Promise<Round> => {
    const { data } = await apiClient.put<ApiResponse<Round>>(
      `/rounds/${roundId}/status`,
      { status }
    );
    return data.data;
  },

  /**
   * 라운드 참가자 목록 조회
   */
  getParticipants: async (roundId: number): Promise<RoundParticipant[]> => {
    const { data } = await apiClient.get<ApiResponse<RoundParticipant[]>>(
      `/rounds/${roundId}/participants`
    );
    return data.data;
  },

  /**
   * 라운드 참가자 제외
   */
  excludeParticipant: async (roundId: number, participantId: number): Promise<RoundParticipant> => {
    const { data } = await apiClient.put<ApiResponse<RoundParticipant>>(
      `/rounds/${roundId}/participants/${participantId}/exclude`
    );
    return data.data;
  },

  /**
   * 라운드 참가자 포함
   */
  includeParticipant: async (roundId: number, participantId: number): Promise<RoundParticipant> => {
    const { data } = await apiClient.put<ApiResponse<RoundParticipant>>(
      `/rounds/${roundId}/participants/${participantId}/include`
    );
    return data.data;
  },
};
