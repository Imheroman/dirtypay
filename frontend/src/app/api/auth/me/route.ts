import { NextResponse } from 'next/server';
import { getSession } from '@/lib/api';

/**
 * GET /api/auth/me
 * 현재 로그인한 유저 정보 반환
 */
export async function GET() {
  const session = await getSession();

  if (!session.user) {
    return NextResponse.json({
      success: false,
      data: null,
      error: { code: 'AUTH_001', message: '로그인이 필요해요' },
    });
  }

  return NextResponse.json({
    success: true,
    data: { user: session.user },
    error: null,
  });
}
