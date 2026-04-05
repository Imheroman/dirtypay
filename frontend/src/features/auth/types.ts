// 회원 권한 타입
export type MemberRole = 'ADMIN' | 'USER';

// 소셜 로그인 제공자 (추후 확장용)
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'KAKAO';

// 사용자 정보 타입
export interface User {
  id: number;
  email: string;
  name: string;
  profileImage?: string | null;
  role: MemberRole;
  createdDate: string;
  provider?: AuthProvider; // 추후 소셜 로그인용
}

// ===== 회원가입 =====
export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  profileImage?: string;
}

export interface SignupResponse {
  id: number;
  email: string;
  name: string;
  profileImage?: string | null;
  role: MemberRole;
  createdDate: string;
}

// ===== 로그인 =====
export interface LoginRequest {
  email: string;
  password: string;
}

// 백엔드 인증 응답 (내부 사용)
export interface AuthResponse {
  accessToken?: string;          // body에 포함될 수 있음 (Set-Cookie 폴백)
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresIn: number;
  refreshTokenExpiresIn: number;
  user: User;
}

// ===== iron-session 세션 데이터 =====
export interface SessionData {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// ===== 토큰 검증 응답 =====
export interface ValidateTokenResponse {
  valid: boolean;
  expiresInSeconds: number;
  message: string;
}

// ===== Auth 상태 (클라이언트) =====
export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

// ===== API 에러 응답 =====
export interface AuthErrorResponse {
  success: false;
  data: null;
  error: {
    code: string;
    message: string;
  };
}
