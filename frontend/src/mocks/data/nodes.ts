import type { Node } from '@/features/organization/types';

/**
 * Session 1에 대한 Mock Node 데이터
 * 트리 구조: 개발팀 > (프론트엔드팀, 백엔드팀), 디자인팀
 */
export const mockNodes: Node[] = [
  // Root 노드 (depth: 0)
  {
    id: 1,
    sessionId: 1,
    parentNodeId: null,
    name: '개발팀',
    depth: 0,
    sortOrder: 1,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  {
    id: 2,
    sessionId: 1,
    parentNodeId: null,
    name: '디자인팀',
    depth: 0,
    sortOrder: 2,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // 개발팀 하위 (depth: 1)
  {
    id: 3,
    sessionId: 1,
    parentNodeId: 1,
    name: '프론트엔드팀',
    depth: 1,
    sortOrder: 1,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  {
    id: 4,
    sessionId: 1,
    parentNodeId: 1,
    name: '백엔드팀',
    depth: 1,
    sortOrder: 2,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-10T09:00:00Z',
    updatedDate: '2024-01-10T09:00:00Z',
  },
  // Session 2 노드
  {
    id: 5,
    sessionId: 2,
    parentNodeId: null,
    name: '전체 참가자',
    depth: 0,
    sortOrder: 1,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-25T10:00:00Z',
    updatedDate: '2024-01-25T10:00:00Z',
  },
  // Session 3 노드 (신년 모임)
  {
    id: 6,
    sessionId: 3,
    parentNodeId: null,
    name: '신년회 참석자',
    depth: 0,
    sortOrder: 1,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
  {
    id: 7,
    sessionId: 3,
    parentNodeId: null,
    name: '가족 동반',
    depth: 0,
    sortOrder: 2,
    isSystem: false,
    isUnassigned: false,
    createdDate: '2024-01-01T10:00:00Z',
    updatedDate: '2024-01-01T10:00:00Z',
  },
];
