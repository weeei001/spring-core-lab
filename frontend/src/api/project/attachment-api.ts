import { http, send } from '@/core/http/client';
import type { Result } from '@/core/http/types';
import { uploadFile } from '@/core/http/upload';
import type { TaskAttachment } from './attachment.types';

const BASE = '/api/task-attachment';

export const attachmentApi = {
  /** 某任務的附件，最多 100 筆 */
  query: (taskId: number) => send(http.post<Result<TaskAttachment[]>>(`${BASE}/query`, { taskId, page: 1, size: 100 })),
  /** 經任務 API 檢查專案權限，再由後端呼叫 basic-data 共用上傳。 */
  upload: (taskId: number, file: File) =>
    uploadFile<TaskAttachment>(`${BASE}/upload`, file, { params: { taskId } }),
  remove: async (id: number) => {
    await send(http.delete<Result<null>>(`${BASE}/${String(id)}`));
  },
  /** 檔案網址，給 img src 與下載連結；圖片在瀏覽器顯示，其他檔案直接下載 */
  contentUrl: (id: number) => `${BASE}/${String(id)}/content`,
};
