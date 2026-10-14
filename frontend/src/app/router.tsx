import { createBrowserRouter } from 'react-router';
import { Result } from 'antd';
import { RequirePermission } from '@/core/auth/RequirePermission';
import { PERMISSIONS } from '@/core/auth/permissions';
import { RolePage, SerialNumberPage, UserPage } from '@/modules/basic-data';
import { BrandPage, ProductPage, TagPage } from '@/modules/product';
import { AppLayout } from './layout/AppLayout';
import { HomePage } from './pages/HomePage';

// 路徑不可用 /api、/auth 開頭，那些會被 vite 轉給 web-server
export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      {
        path: 'products',
        element: (
          <RequirePermission permission={PERMISSIONS.PRODUCT_READ}>
            <ProductPage />
          </RequirePermission>
        ),
      },
      {
        path: 'brands',
        element: (
          <RequirePermission permission={PERMISSIONS.PRODUCT_READ}>
            <BrandPage />
          </RequirePermission>
        ),
      },
      {
        path: 'tags',
        element: (
          <RequirePermission permission={PERMISSIONS.PRODUCT_READ}>
            <TagPage />
          </RequirePermission>
        ),
      },
      {
        path: 'serial-numbers',
        element: (
          <RequirePermission permission={PERMISSIONS.SERIAL_NUMBER_READ}>
            <SerialNumberPage />
          </RequirePermission>
        ),
      },
      {
        path: 'roles',
        element: (
          <RequirePermission permission={PERMISSIONS.ROLE_MANAGE}>
            <RolePage />
          </RequirePermission>
        ),
      },
      {
        path: 'users',
        element: (
          <RequirePermission permission={PERMISSIONS.ROLE_MANAGE}>
            <UserPage />
          </RequirePermission>
        ),
      },
      { path: '*', element: <Result status="404" title="找不到頁面" /> },
    ],
  },
]);
