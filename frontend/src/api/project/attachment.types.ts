import type { BaseEntity } from '@/core/http/types';

/** 任務附件（後端 TaskAttachment）；上傳者是 createId */
export interface TaskAttachment extends BaseEntity {
  taskId: number;
  fileName: string;
  /** 上傳時的 MIME type */
  contentType: string;
  /** bytes */
  size: number;
}
