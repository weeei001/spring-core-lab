import { useState } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhTW from 'antd/locale/zh_TW';
import dayjs from 'dayjs';
import updateLocale from 'dayjs/plugin/updateLocale';
import 'dayjs/locale/zh-tw';
import { AuthProvider } from '@/core/auth/AuthProvider';
import { queryClient } from './query-client';
import { router } from './router';
import { ThemeModeContext, loadThemeMode, saveThemeMode, type ThemeMode } from './theme-mode';
import { getV4Theme } from './theme-v4';

dayjs.extend(updateLocale);
// zh-tw 語系沒有設定一週的第一天，週選擇器會從週日開始；工時統計都以週一為一週開始
dayjs.updateLocale('zh-tw', { weekStart: 1 });
dayjs.locale('zh-tw');

/** 組裝全域 Provider：深淺色 → antd 語系與主題 → 資料請求 → 登入狀態 → 路由 */
export function App() {
  const [mode, setMode] = useState<ThemeMode>(loadThemeMode);
  const toggle = (): void => {
    const next = mode === 'dark' ? 'light' : 'dark';
    setMode(next);
    saveThemeMode(next);
  };

  return (
    <ThemeModeContext.Provider value={{ mode, toggle }}>
      <ConfigProvider locale={zhTW} theme={getV4Theme(mode)}>
        <AntdApp>
          <QueryClientProvider client={queryClient}>
            <AuthProvider>
              <RouterProvider router={router} />
            </AuthProvider>
          </QueryClientProvider>
        </AntdApp>
      </ConfigProvider>
    </ThemeModeContext.Provider>
  );
}
