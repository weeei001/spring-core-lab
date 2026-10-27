import { http, send } from '@/core/http/client';
import type { PageQuery, PageResponse, Result } from '@/core/http/types';
import type { Meeting, MeetingInput, MeetingQuery, MeetingTaskInput, MeetingTaskView } from './meeting.types';

export const meetingApi = {
  query: (query: MeetingQuery) => send(http.post<Result<Meeting[]>>('/api/meeting/query', { ...query, page: 1, size: 100 })),
  queryPage: (query: MeetingQuery & PageQuery) => send(http.post<Result<PageResponse<Meeting>>>('/api/meeting/queryPage', query)),
  get: (id: number) => send(http.get<Result<Meeting>>(`/api/meeting/${String(id)}`)),
  create: (body: MeetingInput & { projectId: number }) => send(http.post<Result<Meeting>>('/api/meeting', body)),
  update: (id: number, body: Partial<MeetingInput>) => send(http.patch<Result<Meeting>>(`/api/meeting/${String(id)}`, body)),
  tasks: (id: number) => send(http.get<Result<MeetingTaskView[]>>(`/api/meeting/${String(id)}/tasks`)),
  createTask: (id: number, body: MeetingTaskInput) => send(http.post<Result<MeetingTaskView>>(`/api/meeting/${String(id)}/tasks`, body)),
  linkTask: (id: number, taskId: number) => send(http.post<Result<Meeting>>(`/api/meeting/${String(id)}/tasks/${String(taskId)}`)),
  unlinkTask: (id: number, taskId: number) => send(http.delete<Result<Meeting>>(`/api/meeting/${String(id)}/tasks/${String(taskId)}`)),
};
