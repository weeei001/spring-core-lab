import type { BaseEntity } from '@/core/http/types';

/** 工時（後端 WorkLog）；只能改、刪自己的 */
export interface WorkLog extends BaseEntity {
  /** 由任務帶入 */
  projectId: number;
  taskId: number;
  /** 登記者，由後端填入 */
  userId: string;
  /** YYYY-MM-DD */
  workDate: string;
  /** 以 0.5 小時為單位 */
  hours: number;
  note: string | null;
}

export interface WorkLogQuery {
  ids?: number[];
  projectId?: number;
  taskId?: number;
  /** 沒給 projectId、taskId 時後端一律改成登入者 */
  userId?: string;
  fromDate?: string;
  toDate?: string;
}

/** 某人某天的工時合計 */
export interface WorkLogSummary {
  userId: string;
  workDate: string;
  hours: number;
}
