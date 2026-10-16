import type { BaseEntity } from '@/core/http/types';

/** 規劃中 → 進行中 → 已結束；同專案同時只能有一個進行中 */
export type SprintStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED';

/** Sprint（後端 Sprint）；狀態只能用 sprintApi.start／close 改 */
export interface Sprint extends BaseEntity {
  projectId: number;
  name: string;
  goal: string | null;
  /** YYYY-MM-DD */
  startDate: string;
  endDate: string;
  status: SprintStatus;
  /** 以下只有查詢結果有：主任務數、完成數與故事點數合計（取消的不計） */
  taskCount?: number;
  doneTaskCount?: number;
  totalPoints?: number;
  donePoints?: number;
}

export interface SprintQuery {
  ids?: number[];
  projectId?: number;
  projectIds?: number[];
  statuses?: SprintStatus[];
}

/** 燃盡圖的每日紀錄（後端 SprintBurndown）；只有任務有異動的日子才有資料 */
export interface SprintBurndown extends BaseEntity {
  sprintId: number;
  /** YYYY-MM-DD */
  day: string;
  taskCount: number;
  doneTaskCount: number;
  totalPoints: number;
  donePoints: number;
}
