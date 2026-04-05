import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  Node,
  NodeTree,
  Member,
  CreateNodeRequest,
  UpdateNodeRequest,
  MoveNodeRequest,
  CreateMemberRequest,
  UpdateMemberRequest,
} from './types';

export const nodeApi = {
  /**
   * 세션의 노드 트리 조회
   */
  getNodes: async (sessionId: number): Promise<NodeTree[]> => {
    const { data } = await apiClient.get<ApiResponse<NodeTree[]>>(
      `/sessions/${sessionId}/nodes`
    );
    return data.data;
  },

  /**
   * 노드 생성
   */
  createNode: async (sessionId: number, request: CreateNodeRequest): Promise<Node> => {
    const { data } = await apiClient.post<ApiResponse<Node>>(
      `/sessions/${sessionId}/nodes`,
      request
    );
    return data.data;
  },

  /**
   * 노드 수정
   */
  updateNode: async (id: number, request: UpdateNodeRequest): Promise<Node> => {
    const { data } = await apiClient.put<ApiResponse<Node>>(
      `/nodes/${id}`,
      request
    );
    return data.data;
  },

  /**
   * 노드 삭제
   */
  deleteNode: async (id: number): Promise<void> => {
    await apiClient.delete(`/nodes/${id}`);
  },

  /**
   * 노드 이동 (부모 변경)
   */
  moveNode: async (nodeId: number, request: MoveNodeRequest): Promise<Node> => {
    const { data } = await apiClient.put<ApiResponse<Node>>(
      `/nodes/${nodeId}/move`,
      request
    );
    return data.data;
  },
};

export const memberApi = {
  /**
   * 세션의 전체 멤버 조회
   */
  getMembers: async (sessionId: number): Promise<Member[]> => {
    const { data } = await apiClient.get<ApiResponse<Member[]>>(
      `/sessions/${sessionId}/members`
    );
    return data.data;
  },

  /**
   * 멤버 생성
   */
  createMember: async (sessionId: number, request: CreateMemberRequest): Promise<Member> => {
    const { data } = await apiClient.post<ApiResponse<Member>>(
      `/sessions/${sessionId}/members`,
      request
    );
    return data.data;
  },

  /**
   * 멤버 수정
   */
  updateMember: async (id: number, request: UpdateMemberRequest): Promise<Member> => {
    const { data } = await apiClient.put<ApiResponse<Member>>(
      `/members/${id}`,
      request
    );
    return data.data;
  },

  /**
   * 멤버 삭제
   */
  deleteMember: async (id: number): Promise<void> => {
    await apiClient.delete(`/members/${id}`);
  },

  /**
   * 멤버-회원 연결 (link)
   */
  linkMember: async (id: number): Promise<Member> => {
    const { data } = await apiClient.patch<ApiResponse<Member>>(
      `/members/${id}/link`
    );
    return data.data;
  },

};
