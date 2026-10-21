import { PERMISSIONS, type PermissionCode } from '@/core/auth/permissions';

export interface MenuPage {
  path: string;
  label: string;
  /** 看得到這頁需要的權限；路由也用同一個權限擋（見 router.tsx） */
  permission: PermissionCode;
}

export interface MenuGroup {
  label: string;
  pages: MenuPage[];
}

/** 側邊選單，依模組分組；沒有權限的頁面不顯示 */
export const MENU: MenuGroup[] = [
  {
    label: '專案',
    pages: [
      { path: '/dashboard', label: '儀表板', permission: PERMISSIONS.PROJECT_READ },
      { path: '/calendar', label: '團隊行事曆', permission: PERMISSIONS.PROJECT_READ },
      { path: '/projects', label: '專案', permission: PERMISSIONS.PROJECT_READ },
      { path: '/my-tasks', label: '我的任務', permission: PERMISSIONS.PROJECT_READ },
      { path: '/timesheet', label: '工時', permission: PERMISSIONS.PROJECT_READ },
    ],
  },
  {
    label: '請假',
    pages: [
      { path: '/leave/requests', label: '我的請假', permission: PERMISSIONS.LEAVE_APPLY },
      { path: '/leave/reviews', label: '請假審核', permission: PERMISSIONS.LEAVE_APPROVE },
    ],
  },
  {
    label: '基本資料',
    pages: [{ path: '/serial-numbers', label: '編號規則', permission: PERMISSIONS.SERIAL_NUMBER_READ }],
  },
  {
    label: '系統管理',
    pages: [
      { path: '/roles', label: '角色', permission: PERMISSIONS.ROLE_MANAGE },
      { path: '/users', label: '使用者', permission: PERMISSIONS.ROLE_MANAGE },
      { path: '/announcements', label: '發送公告', permission: PERMISSIONS.NOTIFICATION_SEND },
    ],
  },
];
