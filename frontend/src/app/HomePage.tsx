import { Navigate } from 'react-router';
import { Result } from 'antd';
import { useAuth } from '@/core/auth/useAuth';
import { MENU } from './menu';

/** 首頁：導向第一個有權限的頁面；一個都沒有時請使用者聯絡管理員 */
export function HomePage() {
  const { can } = useAuth();
  const first = MENU.flatMap((group) => group.pages).find((page) => can(page.permission));
  if (first === undefined) {
    return <Result status="info" title="尚未開通任何功能" subTitle="請聯絡管理員為你指派角色。" />;
  }
  return <Navigate to={first.path} replace />;
}
