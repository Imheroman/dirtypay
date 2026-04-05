import type {
  SignupRequest,
  SignupResponse,
  LoginRequest,
  User,
} from './types';

interface AuthApiResponse<T> {
  success: boolean;
  data: T | null;
  error?: {
    code: string;
    message: string;
  };
}

/**
 * 인증 API
 * - BFF 패턴: Next.js API Routes(/api/auth/*)를 직접 호출
 * - 토큰 관리: 서버 사이드 iron-session에서 처리
 */
export const authApi = {
  /**
   * 회원가입
   */
  signup: async (request: SignupRequest): Promise<SignupResponse> => {
    const res = await fetch('/api/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    const data: AuthApiResponse<SignupResponse> = await res.json();

    if (!res.ok || !data.success || !data.data) {
      throw new Error(data.error?.message || '회원가입에 실패했어요');
    }

    return data.data;
  },

  /**
   * 로그인
   */
  login: async (request: LoginRequest): Promise<{ user: User }> => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    const data: AuthApiResponse<{ user: User }> = await res.json();

    if (!res.ok || !data.success || !data.data) {
      throw new Error(data.error?.message || '로그인에 실패했어요');
    }

    return data.data;
  },

  /**
   * 로그아웃
   */
  logout: async (): Promise<void> => {
    await fetch('/api/auth/logout', { method: 'POST' });
  },

  /**
   * 현재 유저 정보 조회
   */
  me: async (): Promise<User | null> => {
    const res = await fetch('/api/auth/me');
    const data: AuthApiResponse<{ user: User }> = await res.json();

    if (!res.ok || !data.success || !data.data) {
      return null;
    }

    return data.data.user;
  },
};
