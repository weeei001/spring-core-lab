import { createContext } from 'react';
import type { CurrentUser } from './auth-api';
import type { PermissionCode } from './permissions';

export interface AuthContextValue {
  user: CurrentUser;
  /** 目前登入者是否有這個功能權限 */
  can: (permission: PermissionCode) => boolean;
  /** 登出本站；提供者支援 OIDC 登出時一併登出 */
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
