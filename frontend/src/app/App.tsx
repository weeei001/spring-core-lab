import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhTW from 'antd/locale/zh_TW';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-tw';
import { AuthProvider } from '@/core/auth/AuthProvider';
import { queryClient } from './query-client';
import { router } from './router';

dayjs.locale('zh-tw');

/** 組裝全域 Provider：antd 語系 → 資料請求 → 登入狀態 → 路由 */
export function App() {
  return (
    <ConfigProvider locale={zhTW}>
      <AntdApp>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </QueryClientProvider>
      </AntdApp>
    </ConfigProvider>
  );
}
