import { NextRequest, NextResponse } from 'next/server';
import { createSession, extractAccessToken } from '@/lib/api';
import type { LoginRequest, AuthResponse } from '@/features/auth/types';
import type { ApiResponse } from '@/types/api';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

/**
 * POST /api/auth/login
 * 로그인 처리 - 백엔드 호출 후 iron-session에 토큰 저장
 */
export async function POST(request: NextRequest) {
  try {
    const body: LoginRequest = await request.json();

    // 백엔드 로그인 API 호출
    const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({ message: '로그인에 실패했어요' }));
      return NextResponse.json(
        {
          success: false,
          data: null,
          error: { code: 'AUTH_001', message: error.error?.message || error.message || '로그인에 실패했어요' },
        },
        { status: res.status }
      );
    }

    const data: ApiResponse<AuthResponse> = await res.json();

    if (!data.success || !data.data) {
      return NextResponse.json(
        {
          success: false,
          data: null,
          error: data.error || { code: 'AUTH_001', message: '로그인에 실패했어요' },
        },
        { status: 401 }
      );
    }

    // Access Token 추출: Set-Cookie 우선, body 폴백
    const accessToken = extractAccessToken(res.headers.get('set-cookie'))
      || data.data.accessToken
      || '';

    if (!accessToken) {
      console.error('[auth] 로그인 성공했지만 accessToken을 추출할 수 없음.',
        'Set-Cookie:', res.headers.get('set-cookie')?.slice(0, 80) ?? 'null',
        'body keys:', Object.keys(data.data));
      return NextResponse.json(
        {
          success: false,
          data: null,
          error: { code: 'AUTH_002', message: '인증 토큰을 받지 못했어요. 잠시 후 다시 시도해 주세요.' },
        },
        { status: 500 }
      );
    }

    const { user } = data.data;

    // iron-session에 토큰 저장
    await createSession({
      accessToken,
      refreshToken: data.data.refreshToken,
      user,
    });

    // 클라이언트에는 유저 정보만 반환 (토큰은 반환하지 않음)
    return NextResponse.json({
      success: true,
      data: { user },
      error: null,
    });
  } catch (error) {
    console.error('로그인 처리 중 오류:', error);
    return NextResponse.json(
      {
        success: false,
        data: null,
        error: { code: 'COMMON_001', message: '로그인 처리 중 오류가 발생했어요' },
      },
      { status: 500 }
    );
  }
}
