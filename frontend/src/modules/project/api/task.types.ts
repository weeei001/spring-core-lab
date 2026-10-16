import type { BaseEntity } from '@/core/http/types';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
/** 可用 clearFields 清空的欄位 */
export type ClearableTaskField = 'milestoneId' | 'assigneeId' | 'startDate' | 'dueDate' | 'estimateHours' | 'description';

/** 任務（後端 Task）；parentId 有值的是子任務 */
export interface Task extends BaseEntity {
  /** 系統編號，例如 T26100001 */
  code: string;
  projectId: number;
  milestoneId: number | null;
  parentId: number | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  /** 指派人的 user id */
  assigneeId: string | null;
  /** YYYY-MM-DD */
  startDate: string | null;
  dueDate: string | null;
  estimateHours: number | null;
  /** 看板同一欄內的順序 */
  sortOrder: number;
  predecessorIds: number[];
  /** 只在修改時送：要清空的欄位 */
  clearFields?: ClearableTaskField[];
  /** 以下只有查詢結果有 */
  subtaskCount?: number;
  doneSubtaskCount?: number;
  loggedHours?: number;
}

export interface TaskQuery {
  ids?: number[];
  projectId?: number;
  milestoneId?: number;
  parentId?: number;
  /** true 只查主任務、false 只查子任務 */
  topLevel?: boolean;
  statuses?: TaskStatus[];
  priorities?: TaskPriority[];
  assigneeId?: string;
  /** code 或 title 包含的文字；不可為空字串 */
  keyword?: string;
  fromDueDate?: string;
  toDueDate?: string;
}
