import { http, send } from '@/core/http/client';
import type { Result } from '@/core/http/types';

/** 目前登入者（後端 /api/auth/me 回傳的 User，只取需要的欄位） */
export interface CurrentUser {
  id: number;
  subject: string;
  username: string | null;
  name: string;
  email: string | null;
  /** 功能權限代碼 */
  permissions: string[];
}

export const authApi = {
  me: () => send(http.get<Result<CurrentUser>>('/api/auth/me')),
  /** 清掉 web-server 的 session，回傳 authentik 的登出網址 */
  logout: () => send(http.post<Result<string>>('/auth/logout')),
};
