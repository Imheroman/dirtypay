'use server';

import { redirect } from 'next/navigation';
import { getSession, createSession, clearSession, fetchFromBackend, extractAccessToken } from './api';
import type { LoginRequest, AuthResponse, SignupRequest, SignupResponse, User } from '@/features/auth/types';
import type { ApiResponse } from '@/types/api';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

// ============================================
// 로그인
// ============================================

export interface LoginResult {
  success: boolean;
  user?: User;
  error?: string;
}

/**
 * 로그인 서버 액션
 */
export async function login(credentials: LoginRequest): Promise<LoginResult> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({ message: '로그인에 실패했어요' }));
      return {
        success: false,
        error: error.error?.message || error.message || '로그인에 실패했어요',
      };
    }

    const data: ApiResponse<AuthResponse> = await res.json();

    if (!data.success || !data.data) {
      return {
        success: false,
        error: data.error?.message || '로그인에 실패했어요',
      };
    }

    // Access Token 추출: Set-Cookie 우선, body 폴백
    const accessToken = extractAccessToken(res.headers.get('set-cookie'))
      || data.data.accessToken
      || '';

    if (!accessToken) {
      console.error('[auth] 로그인 성공했지만 accessToken을 추출할 수 없음.');
      return {
        success: false,
        error: '인증 토큰을 받지 못했어요. 잠시 후 다시 시도해 주세요.',
      };
    }

    // 임시: 백엔드에서 user 정보를 응답에 포함하지 않는 경우
    // TODO: 백엔드에서 user 정보 포함 시 수정 필요
    const user: User = {
      id: 0, // 임시
      email: credentials.email,
      name: '', // 임시
      role: 'USER',
      createdDate: new Date().toISOString(),
    };

    // 세션에 토큰 저장
    await createSession({
      accessToken,
      refreshToken: data.data.refreshToken,
      user,
    });

    return { success: true, user };
  } catch (error) {
    console.error('로그인 오류:', error);
    return { success: false, error: '서버 오류가 발생했어요' };
  }
}

// ============================================
// 회원가입
// ============================================

export interface SignupResult {
  success: boolean;
  user?: SignupResponse;
  error?: string;
}

/**
 * 회원가입 서버 액션
 */
export async function signup(request: SignupRequest): Promise<SignupResult> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({ message: '회원가입에 실패했어요' }));
      return {
        success: false,
        error: error.error?.message || error.message || '회원가입에 실패했어요',
      };
    }

    const data: ApiResponse<SignupResponse> = await res.json();

    if (!data.success || !data.data) {
      return {
        success: false,
        error: data.error?.message || '회원가입에 실패했어요',
      };
    }

    return { success: true, user: data.data };
  } catch (error) {
    console.error('회원가입 오류:', error);
    return { success: false, error: '서버 오류가 발생했어요' };
  }
}

// ============================================
// 로그아웃
// ============================================

/**
 * 로그아웃 서버 액션
 */
export async function logout(): Promise<void> {
  const session = await getSession();

  // 백엔드에 로그아웃 알림 (토큰 무효화)
  if (session.accessToken) {
    try {
      await fetchFromBackend('/api/auth/logout', { method: 'POST' });
    } catch (error) {
      // 실패해도 세션은 삭제
      console.error('백엔드 로그아웃 오류:', error);
    }
  }

  await clearSession();
  redirect('/login');
}

// ============================================
// 현재 유저 조회
// ============================================

/**
 * 현재 로그인한 유저 정보 조회
 */
export async function getCurrentUser(): Promise<User | null> {
  const session = await getSession();
  return session.user || null;
}

// ============================================
// 인증 상태 확인
// ============================================

/**
 * 인증 상태 확인
 */
export async function checkAuth(): Promise<boolean> {
  const session = await getSession();
  return !!session.accessToken;
}
