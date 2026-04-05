import { getIronSession } from 'iron-session';
import { cookies } from 'next/headers';
import { sessionOptions } from './session';
import type { SessionData, AuthResponse, User } from '@/features/auth/types';
import type { ApiResponse } from '@/types/api';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

// ============================================
// 세션 관리
// ============================================

/**
 * 현재 세션 가져오기 (서버 컴포넌트/API Route에서 사용)
 */
export async function getSession() {
  return getIronSession<SessionData>(await cookies(), sessionOptions);
}

/**
 * 세션 생성 (로그인 성공 시 호출)
 */
export async function createSession(data: SessionData) {
  const session = await getSession();
  session.accessToken = data.accessToken;
  session.refreshToken = data.refreshToken;
  session.user = data.user;
  await session.save();
}

/**
 * 세션 삭제 (로그아웃 시 호출)
 */
export async function clearSession() {
  const session = await getSession();
  session.destroy();
}

// ============================================
// 토큰 갱신
// ============================================

/**
 * Set-Cookie 헤더에서 Access Token 추출
 * 지원 패턴:
 *   1. access_token=<jwt>;...          (현재 백엔드 형식)
 *   2. Authorization=Bearer <jwt>;...  (레거시 형식)
 */
export function extractAccessToken(setCookieHeader: string | null): string {
  if (!setCookieHeader) return '';

  // 1) access_token=<value> 패턴 (현재 백엔드 형식)
  const tokenMatch = setCookieHeader.match(/access_token=([^;]+)/);
  if (tokenMatch) return tokenMatch[1].trim();

  // 2) Authorization=Bearer <value> 패턴 (레거시 폴백)
  try {
    const decoded = decodeURIComponent(setCookieHeader);
    const bearerMatch = decoded.match(/Authorization=(?:"?)Bearer\s+([^";]+)/);
    if (bearerMatch) return bearerMatch[1].trim();
  } catch {
    const bearerMatch = setCookieHeader.match(/Authorization=Bearer[%20\s]+([^;]+)/);
    if (bearerMatch) return bearerMatch[1].trim();
  }

  console.warn('[auth] Set-Cookie 토큰 추출 실패:', setCookieHeader.slice(0, 80));
  return '';
}

/**
 * Refresh Token으로 새 Access Token 발급
 * 싱글톤 프로미스 패턴: 동시 요청 시 하나의 갱신만 실행
 */
let refreshPromise: Promise<{ accessToken: string; refreshToken: string } | null> | null = null;

async function refreshTokens(refreshToken: string): Promise<{
  accessToken: string;
  refreshToken: string;
} | null> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!res.ok) return null;

      const data: ApiResponse<AuthResponse> = await res.json();

      if (!data.success || !data.data) return null;

      // Access Token: Set-Cookie 우선, body 폴백
      const accessToken = extractAccessToken(res.headers.get('set-cookie'))
        || data.data?.accessToken
        || '';

      return {
        accessToken,
        refreshToken: data.data.refreshToken,
      };
    } catch (error) {
      console.error('토큰 갱신 실패:', error);
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

// ============================================
// 백엔드 API 호출 (토큰 자동 갱신 포함)
// ============================================

interface FetchOptions extends RequestInit {
  skipAuth?: boolean; // 인증 헤더 스킵
}

interface FetchResult<T> {
  data: T | null;
  error: string | null;
  errorCode: string | null;
  status: number;
}

/**
 * 백엔드 API 호출 (서버 사이드)
 * - 자동으로 Authorization 헤더 추가
 * - 401 에러 시 자동 토큰 갱신
 */
export async function fetchFromBackend<T = unknown>(
  path: string,
  options: FetchOptions = {}
): Promise<FetchResult<T>> {
  const { skipAuth = false, ...fetchOptions } = options;

  const session = await getSession();

  // 인증이 필요한데 토큰이 없는 경우
  if (!skipAuth && !session.accessToken) {
    return { data: null, error: '인증이 필요해요', errorCode: null, status: 401 };
  }

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers,
  };

  // Authorization 헤더 추가
  if (!skipAuth && session.accessToken) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${session.accessToken}`;
  }

  // 첫 번째 요청
  let res = await fetch(`${BACKEND_URL}${path}`, {
    ...fetchOptions,
    headers,
  });

  // 401이고 refreshToken이 있으면 갱신 시도
  if (res.status === 401 && session.refreshToken) {
    const newTokens = await refreshTokens(session.refreshToken);

    if (newTokens) {
      // 세션 업데이트
      session.accessToken = newTokens.accessToken;
      session.refreshToken = newTokens.refreshToken;
      await session.save();

      // 재요청
      res = await fetch(`${BACKEND_URL}${path}`, {
        ...fetchOptions,
        headers: {
          ...headers,
          Authorization: `Bearer ${newTokens.accessToken}`,
        },
      });
    } else {
      // 갱신 실패 - 세션 삭제
      session.destroy();
      return { data: null, error: '세션이 만료되었어요', errorCode: null, status: 401 };
    }
  }

  // 응답 처리
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: '알 수 없는 오류가 발생했어요' }));
    return {
      data: null,
      error: errorData.error?.message || errorData.message || '요청 처리에 실패했어요',
      errorCode: errorData.error?.code || errorData.errorCode || null,
      status: res.status,
    };
  }

  if (res.status === 204) {
    return { data: null as T, error: null, errorCode: null, status: res.status };
  }

  const data = await res.json();

  // ApiResponse 형태인 경우 data.data 반환
  if ('success' in data && 'data' in data) {
    return { data: data.data as T, error: null, errorCode: null, status: res.status };
  }

  return { data: data as T, error: null, errorCode: null, status: res.status };
}

/**
 * 현재 유저 정보 조회
 */
export async function getCurrentUser(): Promise<User | null> {
  const session = await getSession();
  return session.user || null;
}

/**
 * 인증 상태 확인
 */
export async function checkAuth(): Promise<boolean> {
  const session = await getSession();
  return !!session.accessToken;
}
