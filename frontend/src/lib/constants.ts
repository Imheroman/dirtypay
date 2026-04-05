// API 관련 상수
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

// 페이지네이션 기본값
export const DEFAULT_PAGE_SIZE = 10;

// 조직도 최대 Depth
export const MAX_NODE_DEPTH = 5;

// Session 상태
export const SESSION_STATUS = {
  ACTIVE: "ACTIVE",
  ARCHIVED: "ARCHIVED",
} as const;

// Round 상태
export const ROUND_STATUS = {
  OPEN: "OPEN",
  CLOSED: "CLOSED",
} as const;

// 라우트 경로
export const ROUTES = {
  HOME: "/",
  SESSIONS: "/sessions",
  SESSION_DETAIL: (id: string) => `/sessions/${id}`,
  SESSION_ORGANIZATION: (id: string) => `/sessions/${id}/organization`,
  ROUND_DETAIL: (sessionId: string, roundId: string) =>
    `/sessions/${sessionId}/rounds/${roundId}`,
} as const;
