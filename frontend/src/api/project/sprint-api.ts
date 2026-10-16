import { http, send } from '@/core/http/client';
import { createCrudApi } from '@/core/http/crud';
import type { Result } from '@/core/http/types';
import type { Sprint, SprintBurndown, SprintQuery } from './sprint.types';

const BASE = '/api/sprint';

export const sprintApi = {
  ...createCrudApi<Sprint, SprintQuery>(BASE),
  /** 開始：必須是規劃中，且同專案沒有其他進行中的 Sprint */
  start: (id: number) => send(http.post<Result<Sprint>>(`${BASE}/${String(id)}/start`)),
  /** 結束：未完成的任務移到 moveToSprintId，不給則移回 Backlog */
  close: (id: number, moveToSprintId?: number) =>
    send(http.post<Result<Sprint>>(`${BASE}/${String(id)}/close`, null, { params: { moveToSprintId } })),
  burndown: (id: number) => send(http.get<Result<SprintBurndown[]>>(`${BASE}/${String(id)}/burndown`)),
};
