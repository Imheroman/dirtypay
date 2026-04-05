// User 기본 타입은 auth에서 재사용
export type { User, MemberRole } from '@/features/auth/types';

// ===== 회원 정보 수정 =====
export interface UpdateUserRequest {
  name: string;
  profileImage?: string | null;
}

// ===== 회원 정보 응답 =====
// auth의 User 타입과 동일하므로 재사용
// export type UserResponse = User;
