import { NextRequest, NextResponse } from 'next/server';
import { createSession } from '@/lib/api';
import type { User } from '@/features/auth/types';

/**
 * GET /api/auth/callback
 * 소셜 로그인 콜백 처리 (추후 구현)
 * Spring Boot OAuth 완료 후 리다이렉트되는 엔드포인트
 */
export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;

  const accessToken = searchParams.get('accessToken');
  const refreshToken = searchParams.get('refreshToken');
  const userJson = searchParams.get('user');
  const error = searchParams.get('error');

  // 에러 처리
  if (error || !accessToken || !refreshToken || !userJson) {
    return NextResponse.redirect(
      new URL(`/login?error=${error || 'oauth_failed'}`, request.url)
    );
  }

  try {
    const user: User = JSON.parse(decodeURIComponent(userJson));

    // iron-session에 토큰 저장
    await createSession({
      accessToken,
      refreshToken,
      user,
    });

    // 메인 페이지로 리다이렉트
    return NextResponse.redirect(new URL('/', request.url));
  } catch (err) {
    console.error('소셜 로그인 콜백 처리 오류:', err);
    return NextResponse.redirect(
      new URL('/login?error=oauth_failed', request.url)
    );
  }
}
