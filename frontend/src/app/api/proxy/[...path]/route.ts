import { NextRequest, NextResponse } from 'next/server';
import { fetchFromBackend } from '@/lib/api';

type RouteContext = {
  params: Promise<{ path: string[] }>;
};

/**
 * API 프록시
 * 모든 /api/proxy/* 요청을 백엔드로 전달
 * - 자동으로 Authorization 헤더 추가
 * - 401 에러 시 자동 토큰 갱신
 */

/** 쿼리파라미터를 포함한 전체 API 경로 생성 */
function buildApiPath(path: string[], request: NextRequest): string {
  const apiPath = '/api/' + path.join('/');
  const searchParams = request.nextUrl.searchParams.toString();
  return searchParams ? `${apiPath}?${searchParams}` : apiPath;
}

/** 에러 응답 생성 */
function errorResponse(error: string, status: number, errorCode?: string) {
  return NextResponse.json(
    { success: false, data: null, error: { code: errorCode || 'PROXY_ERROR', message: error } },
    { status }
  );
}

/** 성공 응답 생성 */
function successResponse(data: unknown) {
  return NextResponse.json({ success: true, data, error: null });
}

/** 요청 body 파싱 */
async function parseBody(request: NextRequest): Promise<string | undefined> {
  try {
    const body = await request.json();
    return JSON.stringify(body);
  } catch {
    return undefined;
  }
}

// GET
export async function GET(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const fullPath = buildApiPath(path, request);

  const { data, error, errorCode, status } = await fetchFromBackend(fullPath);

  if (error) return errorResponse(error, status, errorCode ?? undefined);
  return successResponse(data);
}

// POST
export async function POST(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const fullPath = buildApiPath(path, request);
  const body = await parseBody(request);

  const { data, error, errorCode, status } = await fetchFromBackend(fullPath, {
    method: 'POST',
    body,
  });

  if (error) return errorResponse(error, status, errorCode ?? undefined);
  return successResponse(data);
}

// PUT
export async function PUT(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const fullPath = buildApiPath(path, request);
  const body = await parseBody(request);

  const { data, error, errorCode, status } = await fetchFromBackend(fullPath, {
    method: 'PUT',
    body,
  });

  if (error) return errorResponse(error, status, errorCode ?? undefined);
  return successResponse(data);
}

// DELETE
export async function DELETE(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const fullPath = buildApiPath(path, request);

  const { data, error, errorCode, status } = await fetchFromBackend(fullPath, {
    method: 'DELETE',
  });

  if (error) return errorResponse(error, status, errorCode ?? undefined);
  return successResponse(data);
}

// PATCH
export async function PATCH(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const fullPath = buildApiPath(path, request);
  const body = await parseBody(request);

  const { data, error, errorCode, status } = await fetchFromBackend(fullPath, {
    method: 'PATCH',
    body,
  });

  if (error) return errorResponse(error, status, errorCode ?? undefined);
  return successResponse(data);
}
