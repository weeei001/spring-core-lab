import { http, send } from '@/core/http/client';
import type { Result } from '@/core/http/types';
import type { TaskAttachment } from './attachment.types';

const BASE = '/api/task-attachment';

/** 單檔上限，與後端 spring.servlet.multipart.max-file-size 一致 */
export const MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;

export const attachmentApi = {
  /** 某任務的附件，最多 100 筆 */
  query: (taskId: number) => send(http.post<Result<TaskAttachment[]>>(`${BASE}/query`, { taskId, page: 1, size: 100 })),
  upload: (taskId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    // axios 看到 FormData 會自己帶 boundary
    return send(
      http.post<Result<TaskAttachment>>(`${BASE}/upload`, form, {
        params: { taskId },
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },
  remove: async (id: number) => {
    await send(http.delete<Result<null>>(`${BASE}/${String(id)}`));
  },
  /** 檔案網址，給 img src 與下載連結；圖片在瀏覽器顯示，其他檔案直接下載 */
  contentUrl: (id: number) => `${BASE}/${String(id)}/content`,
};
