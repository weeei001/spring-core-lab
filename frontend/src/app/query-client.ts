import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import { ApiError } from '@/core/http/client';

/** 查詢、儲存失敗時統一跳出後端的錯誤訊息；401 會導向登入，不另外提示 */
function showError(error: Error): void {
  if (error instanceof ApiError && error.status === 401) {
    return;
  }
  void message.error(error.message);
}

export const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: showError }),
  mutationCache: new MutationCache({ onError: showError }),
  defaultOptions: {
    // 錯誤多半是權限或資料問題，重試沒有意義；切回視窗也不自動重查
    queries: { retry: false, refetchOnWindowFocus: false },
  },
});
