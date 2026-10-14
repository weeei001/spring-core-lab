import type { BaseEntity } from '@/core/http/types';

/** 角色（後端 Role）：一組功能權限，再指派給使用者 */
export interface Role extends BaseEntity {
  /** 例如 WAREHOUSE；建立後不可修改 */
  code: string;
  name: string;
  /** 功能權限代碼，例如 product:write */
  permissionCodes: string[];
}

export interface RoleQuery {
  keyword?: string;
  ids?: number[];
}

/** 可勾選的功能權限（後端 Permission enum） */
export interface Permission {
  code: string;
  /** 畫面分組用，通常是子系統名稱 */
  group: string;
  description: string;
}
