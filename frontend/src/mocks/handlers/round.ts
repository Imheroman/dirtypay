import { http, HttpResponse } from 'msw';
import { mockRounds } from '../data/rounds';
import type { Round, CreateRoundRequest, UpdateRoundRequest } from '@/features/round/types';

let rounds = [...mockRounds];
let nextId = Math.max(...rounds.map(r => r.id)) + 1;

export const roundHandlers = [
  // GET /api/sessions/:sessionId/rounds - 라운드 목록 조회
  http.get('/api/sessions/:sessionId/rounds', ({ params }) => {
    const sessionId = Number(params.sessionId);
    const sessionRounds = rounds.filter(r => r.sessionId === sessionId);

    return HttpResponse.json({
      success: true,
      data: sessionRounds,
      message: null,
    });
  }),

  // GET /api/rounds/:id - 라운드 상세 조회
  http.get('/api/rounds/:id', ({ params }) => {
    const id = Number(params.id);
    const round = rounds.find(r => r.id === id);

    if (!round) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '라운드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    return HttpResponse.json({
      success: true,
      data: round,
      message: null,
    });
  }),

  // POST /api/sessions/:sessionId/rounds - 라운드 생성
  http.post('/api/sessions/:sessionId/rounds', async ({ params, request }) => {
    const sessionId = Number(params.sessionId);
    const body = await request.json() as CreateRoundRequest;
    const now = new Date().toISOString();

    const sessionRounds = rounds.filter(r => r.sessionId === sessionId);
    const maxSortOrder = sessionRounds.length > 0
      ? Math.max(...sessionRounds.map(r => r.sortOrder))
      : 0;

    const newRound: Round = {
      id: nextId++,
      sessionId,
      title: body.title,
      place: body.place,
      roundDate: body.roundDate,
      status: 'OPEN',
      sortOrder: body.sortOrder ?? maxSortOrder + 1,
      totalAmount: 0,
      participantCount: 0,
      createdDate: now,
      updatedDate: now,
    };

    rounds.push(newRound);

    return HttpResponse.json({
      success: true,
      data: newRound,
      message: null,
    }, { status: 201 });
  }),

  // PUT /api/rounds/:id - 라운드 수정
  http.put('/api/rounds/:id', async ({ params, request }) => {
    const id = Number(params.id);
    const body = await request.json() as UpdateRoundRequest;
    const roundIndex = rounds.findIndex(r => r.id === id);

    if (roundIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '라운드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    const updatedRound: Round = {
      ...rounds[roundIndex],
      ...body,
      updatedDate: new Date().toISOString(),
    };

    rounds[roundIndex] = updatedRound;

    return HttpResponse.json({
      success: true,
      data: updatedRound,
      message: null,
    });
  }),

  // DELETE /api/rounds/:id - 라운드 삭제
  http.delete('/api/rounds/:id', ({ params }) => {
    const id = Number(params.id);
    const roundIndex = rounds.findIndex(r => r.id === id);

    if (roundIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '라운드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    rounds = rounds.filter(r => r.id !== id);

    return HttpResponse.json({
      success: true,
      data: null,
      message: '라운드가 삭제되었어요.',
    });
  }),

  // PUT /api/rounds/:id/status - 라운드 상태 변경
  http.put('/api/rounds/:id/status', async ({ params, request }) => {
    const id = Number(params.id);
    const body = await request.json() as { status: 'OPEN' | 'CLOSED' };
    const roundIndex = rounds.findIndex(r => r.id === id);

    if (roundIndex === -1) {
      return HttpResponse.json({
        success: false,
        data: null,
        message: '라운드를 찾을 수 없어요.',
      }, { status: 404 });
    }

    rounds[roundIndex] = {
      ...rounds[roundIndex],
      status: body.status,
      updatedDate: new Date().toISOString(),
    };

    return HttpResponse.json({
      success: true,
      data: rounds[roundIndex],
      message: null,
    });
  }),
];
