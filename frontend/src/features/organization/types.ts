/**
 * Node (조직도 노드) 타입
 * 트리 구조로 조직을 표현하며, 최대 5단계 깊이까지 지원
 */
export interface Node {
  id: number;
  sessionId: number;
  parentNodeId: number | null;
  name: string;
  depth: number; // 0~4
  sortOrder: number;
  isSystem: boolean;
  isUnassigned: boolean;
  createdDate: string;
  updatedDate: string;
}

/**
 * Node 트리 응답 타입 (자식 노드 포함)
 */
export interface NodeTree extends Node {
  children: NodeTree[];
  members: Member[];
}

/**
 * Node 생성 요청
 */
export interface CreateNodeRequest {
  parentNodeId?: number | null;
  name: string;
  sortOrder?: number;
}

/**
 * Node 수정 요청
 */
export interface UpdateNodeRequest {
  name: string;
  sortOrder?: number;
}

/**
 * Node 이동 요청
 */
export interface MoveNodeRequest {
  targetParentNodeId: number | null;
  sortOrder: number;
}

/**
 * Member (멤버) 타입
 */
export interface Member {
  id: number;
  sessionId: number;
  userId: number | null;
  nickname: string;
  isActive: boolean;
  createdDate: string;
  updatedDate: string;
}

/**
 * Member 생성 요청
 */
export interface CreateMemberRequest {
  nickname: string;
  userId?: number | null;
}

/**
 * Member 수정 요청
 */
export interface UpdateMemberRequest {
  nickname: string;
  isActive?: boolean;
}

/** Node 타입 가드 및 헬퍼 함수 */
export function isMember(node: NodeTree | Member): node is Member {
  return 'nickname' in node;
}

/** 미배정 노드 식별 함수 */
export function isUnassignedNode(node: Node | NodeTree): boolean {
  return node.isUnassigned === true;
}

/** 시스템 노드 식별 함수 */
export function isSystemNode(node: Node | NodeTree): boolean {
  return node.isSystem === true;
}

