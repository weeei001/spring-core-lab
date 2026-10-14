import { http, send } from '@/core/http/client';
import type { PageQuery, PageResponse, Result } from '@/core/http/types';
import type { User, UserQuery } from './user.types';

const BASE = '/api/basic-data/user';

export const userApi = {
  /** 不分頁，最多 100 筆；用來把 createId、transId 換成名字，登入就能用 */
  query: (query: UserQuery) => send(http.post<Result<User[]>>(`${BASE}/query`, { ...query, page: 1, size: 100 })),
  /** 需要 role:manage */
  queryPage: (query: UserQuery & PageQuery) => send(http.post<Result<PageResponse<User>>>(`${BASE}/queryPage`, query)),
  /** 整批取代使用者的角色；需要 role:manage */
  updateRoles: (id: number, roleIds: number[]) =>
    send(http.put<Result<User>>(`${BASE}/${String(id)}/roles`, roleIds)),
};
