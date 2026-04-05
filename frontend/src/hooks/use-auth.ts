'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import type { User, LoginRequest, SignupRequest } from '@/features/auth/types';
import type { ApiResponse } from '@/types/api';

interface UseAuthReturn {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<{ success: boolean; error?: string }>;
  signup: (request: SignupRequest) => Promise<{ success: boolean; error?: string }>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  updateUser: (user: User) => void;
}

/**
 * 인증 훅
 * - /api/auth/me 호출하여 유저 정보 가져오기
 * - /api/auth/login, /api/auth/logout, /api/auth/signup 호출
 */
export function useAuth(): UseAuthReturn {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 현재 유저 정보 조회
  const refresh = useCallback(async () => {
    try {
      const res = await fetch('/api/auth/me');
      const data: ApiResponse<{ user: User }> = await res.json();

      if (res.ok && data.success && data.data) {
        setUser(data.data.user);
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 초기 로드
  useEffect(() => {
    refresh();
  }, [refresh]);

  // 로그인
  const login = async (
    credentials: LoginRequest
  ): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials),
      });

      const data: ApiResponse<{ user: User }> = await res.json();

      if (!res.ok || !data.success) {
        return {
          success: false,
          error: data.error?.message || '로그인에 실패했어요',
        };
      }

      if (data.data) {
        setUser(data.data.user);
      }

      return { success: true };
    } catch {
      return { success: false, error: '서버 오류가 발생했어요' };
    }
  };

  // 회원가입
  const signup = async (
    request: SignupRequest
  ): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });

      const data = await res.json();

      if (!res.ok || !data.success) {
        return {
          success: false,
          error: data.error?.message || '회원가입에 실패했어요',
        };
      }

      return { success: true };
    } catch {
      return { success: false, error: '서버 오류가 발생했어요' };
    }
  };

  // 유저 정보 직접 갱신 (mutation 응답 데이터 활용)
  const updateUser = useCallback((updated: User) => {
    setUser(updated);
  }, []);

  // 로그아웃
  const logout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } catch {
      // 로컬 상태는 초기화
    }
    setUser(null);
    router.push('/login');
  };

  return {
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    signup,
    logout,
    refresh,
    updateUser,
  };
}
