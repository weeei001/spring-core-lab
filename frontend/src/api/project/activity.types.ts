import type { BaseEntity } from '@/core/http/types';

export type TaskActivityType = 'CREATED' | 'CHANGED' | 'COMMENTED';

/** 任務活動紀錄（後端 TaskActivity），系統自動寫入；操作者是 createId */
export interface TaskActivity extends BaseEntity {
  taskId: number;
  type: TaskActivityType;
  /** CHANGED 時改的欄位，例如 status */
  field: string | null;
  /** 舊值、新值都是文字：enum 名稱、YYYY-MM-DD、user id、里程碑 id */
  oldValue: string | null;
  newValue: string | null;
}
