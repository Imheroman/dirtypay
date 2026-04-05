import { SessionOptions } from 'iron-session';
import type { SessionData } from '@/features/auth/types';

/**
 * iron-session 설정
 * - 세션 쿠키는 암호화되어 저장됨
 * - 브라우저에서 토큰에 직접 접근 불가
 */
export const sessionOptions: SessionOptions = {
  password: process.env.SESSION_SECRET!,
  cookieName: 'dirty_pay_session',
  cookieOptions: {
    // production에서만 secure 활성화
    secure: process.env.NODE_ENV === 'production',
    httpOnly: true,
    sameSite: 'lax' as const,
    maxAge: 60 * 60 * 24 * 7, // 7일 (refreshToken 만료 시간과 동일)
  },
};

// iron-session 타입 확장
declare module 'iron-session' {
  interface IronSessionData extends SessionData {}
}
