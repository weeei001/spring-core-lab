import type { BaseEntity } from '@/core/http/types';

/** 里程碑（後端 Milestone） */
export interface Milestone extends BaseEntity {
  projectId: number;
  name: string;
  description: string | null;
  /** YYYY-MM-DD */
  dueDate: string;
  achieved: boolean;
  /** 以下只有查詢結果有：掛在底下的主任務數、完成數 */
  taskCount?: number;
  doneTaskCount?: number;
}

export interface MilestoneQuery {
  ids?: number[];
  projectId?: number;
  projectIds?: number[];
  achieved?: boolean;
  /** 到期日區間 [fromDueDate, toDueDate]，YYYY-MM-DD */
  fromDueDate?: string;
  toDueDate?: string;
}
