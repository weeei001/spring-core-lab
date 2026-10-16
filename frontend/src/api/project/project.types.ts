import type { BaseEntity } from '@/core/http/types';
import type { ProjectRole } from './member.types';
import type { TaskStatus } from './task.types';

export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'DONE' | 'CANCELLED';

/** 專案（後端 Project）；建立者自動成為經理 */
export interface Project extends BaseEntity {
  /** 系統編號，例如 PRJ2610001 */
  code: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  /** YYYY-MM-DD */
  startDate: string | null;
  endDate: string | null;
  /** 例如 #1677FF */
  color: string | null;
  /** 看板 WIP 上限：該欄最多幾張主任務；只能設 IN_PROGRESS、REVIEW，超過時亮紅燈 */
  wipLimits: Partial<Record<TaskStatus, number>>;
  /** 以下只有查詢結果有：主任務數、完成數、逾期數（不含取消） */
  taskCount?: number;
  doneTaskCount?: number;
  overdueTaskCount?: number;
}

export interface ProjectQuery {
  ids?: number[];
  /** code 或 name 包含的文字；不可為空字串 */
  keyword?: string;
  statuses?: ProjectStatus[];
  /** 只查此使用者參與的；沒有 project:admin 時後端一律改成登入者 */
  memberUserId?: string;
  /** 搭配 memberUserId，例如只查我是經理的 */
  memberRole?: ProjectRole;
}
