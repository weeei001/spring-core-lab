import type { ReactNode } from 'react';
import { Result } from 'antd';
import type { PermissionCode } from './permissions';
import { useAuth } from './useAuth';

/** 沒有權限時顯示 403，不渲染頁面（避免頁面一載入就呼叫沒有權限的 API） */
export function RequirePermission({ permission, children }: { permission: PermissionCode; children: ReactNode }) {
  const { can } = useAuth();
  if (!can(permission)) {
    return <Result status="403" title="沒有權限" subTitle="你沒有這個功能的權限，請聯絡管理員指派角色。" />;
  }
  return children;
}
