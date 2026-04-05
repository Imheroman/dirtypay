// API 공통 응답 타입
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
  error?: {
    code: string;
    message: string;
  };
}

// 에러 응답 타입
export interface ApiErrorResponse {
  success: false;
  message: string;
  errorCode: string;
  timestamp: string;
}

// 페이지네이션 요청 파라미터
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}

// 페이지네이션 응답
export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
