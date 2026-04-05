// 공통 ID 타입
export type ID = string | number;

// Nullable 타입
export type Nullable<T> = T | null;

// 기본 엔티티 인터페이스 (BaseEntity 대응)
export interface BaseEntity {
  id: ID;
  createdDate: string;
  updatedDate: string;
  deletedDate?: string | null;
}

// 상태 ENUM 타입들
export type SessionStatus = "ACTIVE" | "ARCHIVED";
export type RoundStatus = "OPEN" | "CLOSED";
