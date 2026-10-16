import type { BaseEntity } from '@/core/http/types';

/** 專案標籤（後端 ProjectLabel）；同專案名稱不可重複 */
export interface ProjectLabel extends BaseEntity {
  projectId: number;
  name: string;
  /** 例如 #1677FF */
  color: string;
  /** 以下只有查詢結果有：掛了這個標籤的任務數（含子任務，取消的不計） */
  taskCount?: number;
}

export interface ProjectLabelQuery {
  ids?: number[];
  projectId?: number;
  projectIds?: number[];
}
