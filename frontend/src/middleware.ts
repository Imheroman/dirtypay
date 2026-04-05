import { NextRequest, NextResponse } from 'next/server';

// 인증이 필요한 경로 (로그인 필요)
const protectedRoutes = [
  '/sessions',
  '/profile',
  '/settings',
  '/join',
  '/wallet',
];

// 인증된 사용자가 접근하면 안 되는 경로 (비로그인 전용)
const authRoutes = ['/login', '/signup'];

// 세션 쿠키 이름 (lib/session.ts와 동일해야 함)
const SESSION_COOKIE_NAME = 'dirty_pay_session';

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // API 라우트, 정적 파일 등은 미들웨어 스킵
  if (
    pathname.startsWith('/_next') ||
    pathname.startsWith('/api') ||
    pathname.includes('.')
  ) {
    return NextResponse.next();
  }

  // 세션 쿠키 존재 여부로 인증 상태 확인
  // 실제 토큰 검증은 API 호출 시 서버에서 처리
  const sessionCookie = request.cookies.get(SESSION_COOKIE_NAME);
  const isAuthenticated = !!sessionCookie?.value;

  // 보호된 경로 접근 시 (로그인 필요)
  if (protectedRoutes.some((route) => pathname.startsWith(route))) {
    if (!isAuthenticated) {
      const loginUrl = new URL('/login', request.url);
      loginUrl.searchParams.set('callbackUrl', pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  // 이미 로그인된 사용자가 로그인/회원가입 페이지 접근 시
  if (authRoutes.some((route) => pathname.startsWith(route))) {
    if (isAuthenticated) {
      return NextResponse.redirect(new URL('/', request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - api routes (handled separately)
     * - public files (images, etc.)
     */
    '/((?!_next/static|_next/image|favicon.ico|api|.*\\..*).*)',
  ],
};
