import type { Member } from '@/features/organization/types';

/**
 * Mock Member 데이터
 */
export const mockMembers: Member[] = [
  // 개발팀 (nodeId: 1) - 팀장
  {
    id: 1,
    sessionId: 1,
    userId: 1,
    nickname: '김개발',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // 프론트엔드팀 (nodeId: 3)
  {
    id: 2,
    sessionId: 3,
    userId: 2,
    nickname: '이프론트',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  {
    id: 3,
    sessionId: 3,
    userId: 3,
    nickname: '박리액트',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // 백엔드팀 (nodeId: 4)
  {
    id: 4,
    sessionId: 4,
    userId: 4,
    nickname: '최백엔드',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  {
    id: 5,
    sessionId: 4,
    userId: 5,
    nickname: '정스프링',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // 디자인팀 (nodeId: 2)
  {
    id: 6,
    sessionId: 2,
    userId: 6,
    nickname: '한디자인',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  {
    id: 7,
    sessionId: 2,
    userId: null,
    nickname: '오피그마',
    isActive: true,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // Session 2 멤버 (nodeId: 5)
  {
    id: 8,
    sessionId: 5,
    userId: 1,
    nickname: '김개발',
    isActive: true,
    createdDate: '2024-01-25T10:00:00Z',
    updatedDate: '2024-01-25T10:00:00Z',
  },
  {
    id: 9,
    sessionId: 5,
    userId: 2,
    nickname: '이프론트',
    isActive: true,
    createdDate: '2024-01-25T10:00:00Z',
    updatedDate: '2024-01-25T10:00:00Z',
  },
  // Session 3 멤버 - 신년회 참석자 (nodeId: 6)
  {
    id: 10,
    sessionId: 6,
    userId: 1,
    nickname: '김영수',
    isActive: true,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
  {
    id: 11,
    sessionId: 6,
    userId: 2,
    nickname: '이미영',
    isActive: true,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
  {
    id: 12,
    sessionId: 6,
    userId: null,
    nickname: '박철수',
    isActive: true,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
  // Session 3 멤버 - 가족 동반 (nodeId: 7)
  {
    id: 13,
    sessionId: 7,
    userId: null,
    nickname: '김영수 가족',
    isActive: true,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
  {
    id: 14,
    sessionId: 7,
    userId: null,
    nickname: '이미영 가족',
    isActive: true,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
];
