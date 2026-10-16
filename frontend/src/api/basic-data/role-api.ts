import { http, send } from '@/core/http/client';
import { createCrudApi } from '@/core/http/crud';
import type { Result } from '@/core/http/types';
import type { Permission, Role, RoleQuery } from './role.types';

export const roleApi = {
  ...createCrudApi<Role, RoleQuery>('/api/basic-data/role'),
  /** 所有可勾選的功能權限 */
  permissions: () => send(http.get<Result<Permission[]>>('/api/basic-data/role/permissions')),
};
