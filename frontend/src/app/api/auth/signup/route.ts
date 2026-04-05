import { NextRequest, NextResponse } from 'next/server';
import type { SignupRequest, SignupResponse } from '@/features/auth/types';
import type { ApiResponse } from '@/types/api';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

/**
 * POST /api/auth/signup
 * 회원가입 처리
 */
export async function POST(request: NextRequest) {
  try {
    const body: SignupRequest = await request.json();

    // 백엔드 회원가입 API 호출
    const res = await fetch(`${BACKEND_URL}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({ message: '회원가입에 실패했어요' }));
      return NextResponse.json(
        {
          success: false,
          data: null,
          error: { code: 'COMMON_002', message: error.error?.message || error.message || '회원가입에 실패했어요' },
        },
        { status: res.status }
      );
    }

    const data: ApiResponse<SignupResponse> = await res.json();

    if (!data.success || !data.data) {
      return NextResponse.json(
        {
          success: false,
          data: null,
          error: data.error || { code: 'COMMON_002', message: '회원가입에 실패했어요' },
        },
        { status: 400 }
      );
    }

    return NextResponse.json({
      success: true,
      data: data.data,
      error: null,
    });
  } catch (error) {
    console.error('회원가입 처리 중 오류:', error);
    return NextResponse.json(
      {
        success: false,
        data: null,
        error: { code: 'COMMON_001', message: '회원가입 처리 중 오류가 발생했어요' },
      },
      { status: 500 }
    );
  }
}
