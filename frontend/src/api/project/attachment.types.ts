import type { BaseEntity } from '@/core/http/types';

/** 任務附件（後端 TaskAttachment）；上傳者是 createId */
export interface TaskAttachment extends BaseEntity {
  taskId: number;
  /** 共用檔案 id；舊附件沒有此關聯。預覽與下載仍使用任務附件 API。 */
  uploadFileId: number | null;
  fileName: string;
  /** 上傳時的 MIME type */
  contentType: string;
  /** bytes */
  size: number;
}
