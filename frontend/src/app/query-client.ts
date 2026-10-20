import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import { ApiError } from '@/core/http/client';

/**
 * 查詢、儲存失敗時統一跳出後端的錯誤訊息。
 * 401 代表 session 過期，不另外提示，重新取得登入者讓 AuthProvider 顯示登入頁；
 * 登入者本身的查詢 401 由 AuthProvider 處理，不能再重查，否則會一直重複。
 * 403 時也重新取得權限，讓畫面移除已被管理員收回權限的功能。
 */
function showError(error: Error, isMeQuery = false): void {
  if (error instanceof ApiError && error.status === 401) {
    if (!isMeQuery) {
      void queryClient.invalidateQueries({ queryKey: ['me'] });
    }
    return;
  }
  if (error instanceof ApiError && error.status === 403 && !isMeQuery) {
    void queryClient.invalidateQueries({ queryKey: ['me'] });
    // 成員資格與功能權限分開管理；退出專案後也要收回仍顯示的編輯入口。
    queryClient.setQueriesData({ queryKey: ['project-member'] }, []);
    void queryClient.invalidateQueries({ queryKey: ['project-member'], refetchType: 'none' });
    void queryClient.invalidateQueries({ queryKey: ['project'], refetchType: 'none' });
  }
  void message.error(error.message);
}

export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      showError(error, query.queryKey[0] === 'me');
    },
  }),
  mutationCache: new MutationCache({
    onError: (error) => {
      showError(error);
    },
  }),
  defaultOptions: {
    // 錯誤多半是權限或資料問題，重試沒有意義；切回視窗也不自動重查
    queries: { retry: false, refetchOnWindowFocus: false },
  },
});
