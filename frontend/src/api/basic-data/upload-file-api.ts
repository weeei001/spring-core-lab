import { http, send } from '@/core/http/client';
import type { Result } from '@/core/http/types';
import { uploadFile } from '@/core/http/upload';
import type { UploadFile } from './upload-file.types';

const BASE = '/api/basic-data/upload-file';

export const uploadFileApi = {
  /** File 用於一般表單，Blob + fileName 可供編輯器貼上圖片使用。 */
  upload: (file: Blob, fileName?: string, onProgress?: (percent: number) => void) =>
    uploadFile<UploadFile>(`${BASE}/upload`, file, { fileName, onProgress }),
  get: (id: number) => send(http.get<Result<UploadFile>>(`${BASE}/${String(id)}`)),
  remove: async (id: number) => {
    await send(http.delete<Result<null>>(`${BASE}/${String(id)}`));
  },
  /** 需要登入，且只供上傳者存取；業務共用時使用該模組的授權取檔網址。 */
  contentUrl: (id: number) => http.getUri({ url: `${BASE}/${String(id)}/content` }),
};
