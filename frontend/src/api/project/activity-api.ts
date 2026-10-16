import { http, send } from '@/core/http/client';
import type { Result } from '@/core/http/types';
import type { TaskActivity } from './activity.types';

export const activityApi = {
  /** 某任務最早的 100 筆活動（只能查詢） */
  query: (taskId: number) =>
    send(http.post<Result<TaskActivity[]>>('/api/task-activity/query', { taskId, page: 1, size: 100 })),
};
