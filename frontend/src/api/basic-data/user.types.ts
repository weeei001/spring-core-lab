import type { BaseEntity } from '@/core/http/types';

/** 使用者（後端 User）；第一次登入時自動建立 */
export interface User extends BaseEntity {
  /** authentik 的使用者識別 */
  subject: string;
  username: string | null;
  name: string;
  email: string | null;
  roleIds: number[];
}

export interface UserQuery {
  /** 名稱、帳號或 Email 包含的文字；不可為空字串 */
  keyword?: string;
  /** createId、transId 是字串；不可包含 system */
  ids?: string[];
}
