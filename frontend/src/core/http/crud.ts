import { http, send } from './client';
import type { PageQuery, PageResponse, Result } from './types';

/** 各資源共用的 CRUD，對應後端 WebXxxController 的 query、queryPage、create、update、delete */
export interface CrudApi<T, Q> {
  /** 不分頁，最多 100 筆（下拉選單、id 換名稱用） */
  query: (query: Q) => Promise<T[]>;
  queryPage: (query: Q & PageQuery) => Promise<PageResponse<T>>;
  create: (body: Partial<T>) => Promise<T>;
  /** 部分更新：只送要改的欄位，沒給代表不改 */
  update: (id: number, body: Partial<T>) => Promise<T>;
  remove: (id: number) => Promise<void>;
}

// T、Q 只出現在回傳型別：由呼叫端宣告這個資源的型別，對應後端 API 規格
// eslint-disable-next-line @typescript-eslint/no-unnecessary-type-parameters
export function createCrudApi<T, Q extends object>(base: string): CrudApi<T, Q> {
  return {
    // 後端 query 的 page 必須為 1，size 最多 100
    query: (query) => send(http.post<Result<T[]>>(`${base}/query`, { ...query, page: 1, size: 100 })),
    queryPage: (query) => send(http.post<Result<PageResponse<T>>>(`${base}/queryPage`, query)),
    create: (body) => send(http.post<Result<T>>(base, body)),
    update: (id, body) => send(http.patch<Result<T>>(`${base}/${String(id)}`, body)),
    remove: async (id) => {
      await send(http.delete<Result<null>>(`${base}/${String(id)}`));
    },
  };
}
