import { http, send } from '@/core/http/client';
import { createCrudApi } from '@/core/http/crud';
import type { Result } from '@/core/http/types';
import type { Task, TaskQuery, TaskStatus } from './task.types';

const BASE = '/api/task';
const PAGE_SIZE = 100;
/** 一次最多載入 2000 筆，避免異常資料量拖垮畫面 */
const MAX_PAGES = 20;

const crud = createCrudApi<Task, TaskQuery>(BASE);

export const taskApi = {
  ...crud,
  /** 載入全部符合條件的任務（分頁逐頁取），依看板順序排列；專案的三種檢視共用這份資料在前端篩選 */
  queryAll: async (query: TaskQuery): Promise<Task[]> => {
    const all: Task[] = [];
    for (let page = 1; page <= MAX_PAGES; page++) {
      const result = await crud.queryPage({
        ...query,
        page,
        size: PAGE_SIZE,
        sort: [
          { field: 'sortOrder', direction: 'asc' },
          { field: 'id', direction: 'asc' },
        ],
      });
      all.push(...result.items);
      if (result.items.length < PAGE_SIZE || all.length >= result.total) {
        break;
      }
    }
    return all;
  },
  /** 看板拖曳：改成 status，排在 previousId 之後；previousId 不給表示排第一 */
  move: (id: number, status: TaskStatus, previousId?: number) =>
    send(http.post<Result<Task>>(`${BASE}/${String(id)}/move`, null, { params: { status, previousId } })),
  /** 登入者關注任務：之後的狀態變更、新留言會收到通知 */
  watch: (id: number) => send(http.post<Result<Task>>(`${BASE}/${String(id)}/watch`)),
  unwatch: (id: number) => send(http.delete<Result<Task>>(`${BASE}/${String(id)}/watch`)),
};
