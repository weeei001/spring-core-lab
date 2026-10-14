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
    label: '商品',
    pages: [
      { path: '/products', label: '商品', permission: PERMISSIONS.PRODUCT_READ },
      { path: '/brands', label: '品牌', permission: PERMISSIONS.PRODUCT_READ },
      { path: '/tags', label: '標籤', permission: PERMISSIONS.PRODUCT_READ },
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
    ],
  },
];
