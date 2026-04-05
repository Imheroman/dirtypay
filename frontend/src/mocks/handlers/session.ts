import { http, HttpResponse } from 'msw';
import { mockSessions } from '../data/sessions';
import type { Session, CreateSessionRequest, UpdateSessionRequest } from '@/features/session/types';

let sessions = [...mockSessions];
let nextId = Math.max(...sessions.map(s => s.id)) + 1;

export const sessionHandlers = [
  // GET /api/proxy/sessions - 세션 목록 조회
  http.get('/api/proxy/sessions', () => {
    return HttpResponse.json({
      success: true,
      data: sessions,
      message: null,
    });
  }),

  // GET /api/proxy/sessions/:id - 세션 상세 조회
  http.get('/api/proxy/sessions/:id', ({ params }) => {
    const id = Number(params.id);
    const session = sessions.find(s => s.id === id);

    if (!session) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '세션을 찾을 수 없어요.',
      }, { status: 404 });
    }

    return HttpResponse.json({
      success: true,
      data: session,
      message: null,
    });
  }),

  // POST /api/proxy/sessions - 세션 생성
  http.post('/api/proxy/sessions', async ({ request }) => {
    const body = await request.json() as CreateSessionRequest;
    const now = new Date().toISOString();

    const newSession: Session = {
      id: nextId++,
      title: body.title,
      description: body.description,
      startDate: body.startDate,
      endDate: body.endDate,
      status: 'ACTIVE',
      ownerId: 1,
      createdDate: now,
      updatedDate: now,
    };

    sessions.push(newSession);

    return HttpResponse.json({
      success: true,
      data: newSession,
      message: null,
    }, { status: 201 });
  }),

  // PUT /api/proxy/sessions/:id - 세션 수정
  http.put('/api/proxy/sessions/:id', async ({ params, request }) => {
    const id = Number(params.id);
    const body = await request.json() as UpdateSessionRequest;
    const sessionIndex = sessions.findIndex(s => s.id === id);

    if (sessionIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '세션을 찾을 수 없어요.',
      }, { status: 404 });
    }

    const updatedSession: Session = {
      ...sessions[sessionIndex],
      ...body,
      updatedDate: new Date().toISOString(),
    };

    sessions[sessionIndex] = updatedSession;

    return HttpResponse.json({
      success: true,
      data: updatedSession,
      message: null,
    });
  }),

  // DELETE /api/proxy/sessions/:id - 세션 삭제
  http.delete('/api/proxy/sessions/:id', ({ params }) => {
    const id = Number(params.id);
    const sessionIndex = sessions.findIndex(s => s.id === id);

    if (sessionIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '세션을 찾을 수 없어요.',
      }, { status: 404 });
    }

    sessions = sessions.filter(s => s.id !== id);

    return HttpResponse.json({
      success: true,
      data: null,
      message: '세션이 삭제되었어요.',
    });
  }),
];
