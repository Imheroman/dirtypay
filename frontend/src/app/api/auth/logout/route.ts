import { NextResponse } from 'next/server';
import { clearSession, fetchFromBackend } from '@/lib/api';

/**
 * POST /api/auth/logout
 * 로그아웃 처리 - 백엔드 로그아웃 호출 후 iron-session 삭제
 */
export async function POST() {
  try {
    // 백엔드에 로그아웃 알림 (토큰 무효화)
    await fetchFromBackend('/api/auth/logout', { method: 'POST' });
  } catch (error) {
    // 백엔드 호출 실패해도 세션은 삭제
    console.error('백엔드 로그아웃 호출 실패:', error);
  }

  // iron-session 삭제
  await clearSession();

  return NextResponse.json({
    success: true,
    data: null,
    error: null,
  });
}
