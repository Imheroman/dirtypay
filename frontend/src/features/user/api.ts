import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { User } from '@/features/auth/types';
import type { UpdateUserRequest } from './types';

const BASE_URL = '/users';

export const userApi = {
  /**
   * 회원 정보 조회
   */
  getUser: async (id: number): Promise<User> => {
    const { data } = await apiClient.get<ApiResponse<User>>(`${BASE_URL}/${id}`);
    return data.data;
  },

  /**
   * 회원 정보 수정
   */
  updateUser: async (id: number, request: UpdateUserRequest): Promise<User> => {
    const { data } = await apiClient.put<ApiResponse<User>>(
      `${BASE_URL}/${id}`,
      request
    );
    return data.data;
  },

  /**
   * 회원 삭제 (Soft Delete)
   */
  deleteUser: async (id: number): Promise<void> => {
    await apiClient.delete(`${BASE_URL}/${id}`);
  },
};
