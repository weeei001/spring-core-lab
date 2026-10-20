import { ApiError, http, send } from './client';
import type { Result } from './types';

/** 與 web-server、basic-data、project-service 的單檔上限一致。 */
export const MAX_UPLOAD_FILE_SIZE = 10 * 1024 * 1024;

/** 共用 multipart 上傳；各功能傳入自己的授權 API 網址。 */
export async function uploadFile<T>(
  url: string,
  file: Blob,
  options: {
    fileName?: string;
    params?: Record<string, string | number>;
    onProgress?: (percent: number) => void;
  } = {},
): Promise<T> {
  if (file.size === 0) throw new ApiError('檔案是空的');
  if (file.size > MAX_UPLOAD_FILE_SIZE) throw new ApiError('檔案不可超過 10MB');
  const form = new FormData();
  form.append('file', file, options.fileName ?? (file instanceof File ? file.name : 'file'));
  return send(http.post<Result<T>>(url, form, {
    params: options.params,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: ({ loaded, total }) => {
      if (total) options.onProgress?.(Math.min(100, loaded / total * 100));
    },
  }));
}
