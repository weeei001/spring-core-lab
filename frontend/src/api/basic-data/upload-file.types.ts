import type { BaseEntity } from '@/core/http/types';

/** 共用檔案紀錄；業務資料保存 id，實體檔案由 basic-data 管理。 */
export interface UploadFile extends BaseEntity {
  fileName: string;
  contentType: string;
  /** bytes */
  size: number;
  businessManaged: boolean;
}
