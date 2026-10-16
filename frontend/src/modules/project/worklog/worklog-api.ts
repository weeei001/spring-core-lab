import { http, send } from '@/core/http/client';
import { createCrudApi } from '@/core/http/crud';
import type { Result } from '@/core/http/types';
import type { WorkLog, WorkLogQuery, WorkLogSummary } from './worklog.types';

const BASE = '/api/work-log';

export const workLogApi = {
  ...createCrudApi<WorkLog, WorkLogQuery>(BASE),
  /** 依人、日合計；必須給 fromDate、toDate，最多 366 天 */
  summary: (query: WorkLogQuery & { fromDate: string; toDate: string }) =>
    send(http.post<Result<WorkLogSummary[]>>(`${BASE}/summary`, query)),
};
