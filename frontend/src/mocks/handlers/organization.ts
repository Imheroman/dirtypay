import { http, HttpResponse } from 'msw';
import { mockNodes } from '../data/nodes';
import { mockMembers } from '../data/members';
import type {
  Node,
  NodeTree,
  Member,
  CreateNodeRequest,
  UpdateNodeRequest,
  CreateMemberRequest,
  UpdateMemberRequest,
} from '@/features/organization/types';

let nodes = [...mockNodes];
let members = [...mockMembers];
let nextNodeId = Math.max(...nodes.map(n => n.id)) + 1;
let nextMemberId = Math.max(...members.map(m => m.id)) + 1;

/**
 * Node 배열을 트리 구조로 변환
 */
function buildNodeTree(sessionId: number): NodeTree[] {
  const sessionNodes = nodes.filter(n => n.sessionId === sessionId);
  const sessionMembers = members.filter(m =>
    sessionNodes.some(n => n.id === m.sessionId)
  );

  const nodeMap = new Map<number, NodeTree>();

  // 모든 노드를 NodeTree로 변환
  sessionNodes.forEach(node => {
    nodeMap.set(node.id, {
      ...node,
      children: [],
      members: sessionMembers.filter(m => m.sessionId === node.id),
    });
  });

  // 트리 구조 구성
  const rootNodes: NodeTree[] = [];
  sessionNodes.forEach(node => {
    const nodeTree = nodeMap.get(node.id)!;
    if (node.parentNodeId === null) {
      rootNodes.push(nodeTree);
    } else {
      const parent = nodeMap.get(node.parentNodeId);
      if (parent) {
        parent.children.push(nodeTree);
      }
    }
  });

  // sortOrder로 정렬
  const sortChildren = (nodeTree: NodeTree): void => {
    nodeTree.children.sort((a, b) => a.sortOrder - b.sortOrder);
    nodeTree.children.forEach(sortChildren);
  };
  rootNodes.sort((a, b) => a.sortOrder - b.sortOrder);
  rootNodes.forEach(sortChildren);

  return rootNodes;
}

export const organizationHandlers = [
  // ========== Node API ==========

  // GET /api/sessions/:sessionId/nodes - 세션의 노드 트리 조회
  http.get('/api/sessions/:sessionId/nodes', ({ params }) => {
    const sessionId = Number(params.sessionId);
    const nodeTree = buildNodeTree(sessionId);

    return HttpResponse.json({
      success: true,
      data: nodeTree,
      message: null,
    });
  }),

  // POST /api/sessions/:sessionId/nodes - 노드 생성
  http.post('/api/sessions/:sessionId/nodes', async ({ params, request }) => {
    const sessionId = Number(params.sessionId);
    const body = await request.json() as CreateNodeRequest;
    const now = new Date().toISOString();

    // depth 계산
    let depth = 0;
    if (body.parentNodeId) {
      const parentNode = nodes.find(n => n.id === body.parentNodeId);
      if (!parentNode) {
        return HttpResponse.json({
          success: false,
          data: null,
          message: '부모 노드를 찾을 수 없어요.',
        }, { status: 404 });
      }
      depth = parentNode.depth + 1;

      // depth 제한 검증 (최대 5단계, 0~4)
      if (depth > 4) {
        return HttpResponse.json({
          success: false,
          data: null,
          message: '조직도는 최대 5단계까지만 가능해요.',
        }, { status: 400 });
      }
    }

    // sortOrder 계산
    const siblingNodes = nodes.filter(
      n => n.sessionId === sessionId && n.parentNodeId === (body.parentNodeId ?? null)
    );
    const sortOrder = body.sortOrder ?? siblingNodes.length + 1;

    const newNode: Node = {
      id: nextNodeId++,
      sessionId,
      parentNodeId: body.parentNodeId ?? null,
      name: body.name,
      depth,
      sortOrder,
      isSystem: false,
      isUnassigned: false,
      createdDate: now,
      updatedDate: now,
    };

    nodes.push(newNode);

    return HttpResponse.json({
      success: true,
      data: newNode,
      message: null,
    }, { status: 201 });
  }),

  // PUT /api/nodes/:id - 노드 수정
  http.put('/api/nodes/:id', async ({ params, request }) => {
    const id = Number(params.id);
    const body = await request.json() as UpdateNodeRequest;
    const nodeIndex = nodes.findIndex(n => n.id === id);

    if (nodeIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '노드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    const updatedNode: Node = {
      ...nodes[nodeIndex],
      ...body,
      updatedDate: new Date().toISOString(),
    };

    nodes[nodeIndex] = updatedNode;

    return HttpResponse.json({
      success: true,
      data: updatedNode,
      message: null,
    });
  }),

  // DELETE /api/nodes/:id - 노드 삭제 (하위 노드는 부모로 승격)
  http.delete('/api/nodes/:id', ({ params }) => {
    const id = Number(params.id);
    const nodeIndex = nodes.findIndex(n => n.id === id);

    if (nodeIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '노드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    const deletedNode = nodes[nodeIndex];

    // 하위 노드들의 parentNodeId를 삭제되는 노드의 parentNodeId로 변경
    nodes = nodes.map(n => {
      if (n.parentNodeId === id) {
        return {
          ...n,
          parentNodeId: deletedNode.parentNodeId,
          depth: n.depth - 1,
          updatedDate: new Date().toISOString(),
        };
      }
      return n;
    });

    // 노드 삭제
    nodes = nodes.filter(n => n.id !== id);

    return HttpResponse.json({
      success: true,
      data: null,
      message: '노드가 삭제되었어요.',
    });
  }),

  // ========== Member API ==========

  // GET /api/sessions/:sessionId/members - 세션의 전체 멤버 조회
  http.get('/api/sessions/:sessionId/members', ({ params }) => {
    const sessionId = Number(params.sessionId);
    const sessionNodes = nodes.filter(n => n.sessionId === sessionId);
    const sessionMembers = members.filter(m =>
      sessionNodes.some(n => n.id === m.sessionId)
    );

    return HttpResponse.json({
      success: true,
      data: sessionMembers,
      message: null,
    });
  }),

  // POST /api/sessions/:sessionId/members - 멤버 생성
  http.post('/api/sessions/:sessionId/members', async ({ params, request }) => {
    const sessionId = Number(params.sessionId);
    const body = await request.json() as CreateMemberRequest;
    const now = new Date().toISOString();

    const newMember: Member = {
      id: nextMemberId++,
      sessionId,
      userId: body.userId ?? null,
      nickname: body.nickname,
      isActive: true,
      createdDate: now,
      updatedDate: now,
    };

    members.push(newMember);

    return HttpResponse.json({
      success: true,
      data: newMember,
      message: null,
    }, { status: 201 });
  }),

  // PUT /api/members/:id - 멤버 수정
  http.put('/api/members/:id', async ({ params, request }) => {
    const id = Number(params.id);
    const body = await request.json() as UpdateMemberRequest;
    const memberIndex = members.findIndex(m => m.id === id);

    if (memberIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '멤버를 찾을 수 없어요.',
      }, { status: 404 });
    }

    const updatedMember: Member = {
      ...members[memberIndex],
      ...body,
      updatedDate: new Date().toISOString(),
    };

    members[memberIndex] = updatedMember;

    return HttpResponse.json({
      success: true,
      data: updatedMember,
      message: null,
    });
  }),

  // DELETE /api/members/:id - 멤버 삭제
  http.delete('/api/members/:id', ({ params }) => {
    const id = Number(params.id);
    const memberIndex = members.findIndex(m => m.id === id);

    if (memberIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '멤버를 찾을 수 없어요.',
      }, { status: 404 });
    }

    members = members.filter(m => m.id !== id);

    return HttpResponse.json({
      success: true,
      data: null,
      message: '멤버가 삭제되었어요.',
    });
  }),
];
