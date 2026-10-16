import type { TaskActivityType } from '@/api/project/activity.types';
import type { ProjectRole } from '@/api/project/member.types';
import type { ProjectStatus } from '@/api/project/project.types';
import type { SprintStatus } from '@/api/project/sprint.types';
import type { TaskPriority, TaskStatus, TaskType } from '@/api/project/task.types';

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING: '規劃中',
  ACTIVE: '進行中',
  ON_HOLD: '暫停',
  DONE: '結案',
  CANCELLED: '取消',
};

/** antd Tag 的顏色 */
export const PROJECT_STATUS_COLORS: Record<ProjectStatus, string> = {
  PLANNING: 'default',
  ACTIVE: 'processing',
  ON_HOLD: 'warning',
  DONE: 'success',
  CANCELLED: 'error',
};

/** 可以改成哪些狀態，與後端 ProjectStatus.next() 相同 */
export const PROJECT_STATUS_NEXT: Record<ProjectStatus, ProjectStatus[]> = {
  PLANNING: ['ACTIVE', 'ON_HOLD', 'CANCELLED'],
  ACTIVE: ['DONE', 'ON_HOLD', 'CANCELLED'],
  ON_HOLD: ['ACTIVE', 'CANCELLED'],
  DONE: ['ACTIVE'],
  CANCELLED: ['ACTIVE'],
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: '待辦',
  IN_PROGRESS: '進行中',
  REVIEW: '審核中',
  DONE: '完成',
  CANCELLED: '取消',
};

/** 看板欄位的順序 */
export const TASK_STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE', 'CANCELLED'];

/** 看板欄位、甘特圖的顏色 */
export const TASK_STATUS_COLORS: Record<TaskStatus, string> = {
  TODO: '#8c8c8c',
  IN_PROGRESS: '#1890ff',
  REVIEW: '#722ed1',
  DONE: '#52c41a',
  CANCELLED: '#bfbfbf',
};

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '緊急',
};

export const TASK_PRIORITIES: TaskPriority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];

export const TASK_PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: '#bfbfbf',
  MEDIUM: '#1890ff',
  HIGH: '#fa8c16',
  URGENT: '#f5222d',
};

export const TASK_TYPE_LABELS: Record<TaskType, string> = {
  FEATURE: '功能',
  BUG: 'Bug',
  CHORE: '雜務',
  SPIKE: '技術調查',
};

export const TASK_TYPES: TaskType[] = ['FEATURE', 'BUG', 'CHORE', 'SPIKE'];

/** antd Tag 的顏色 */
export const TASK_TYPE_COLORS: Record<TaskType, string> = {
  FEATURE: 'blue',
  BUG: 'red',
  CHORE: 'default',
  SPIKE: 'purple',
};

export const SPRINT_STATUS_LABELS: Record<SprintStatus, string> = {
  PLANNED: '規劃中',
  ACTIVE: '進行中',
  CLOSED: '已結束',
};

/** antd Tag 的顏色 */
export const SPRINT_STATUS_COLORS: Record<SprintStatus, string> = {
  PLANNED: 'default',
  ACTIVE: 'processing',
  CLOSED: 'success',
};

/** 故事點數的常用值（費氏數列） */
export const STORY_POINT_OPTIONS = [1, 2, 3, 5, 8, 13];

export const PROJECT_ROLE_LABELS: Record<ProjectRole, string> = {
  MANAGER: '經理',
  MEMBER: '成員',
};

export const ACTIVITY_TYPE_LABELS: Record<TaskActivityType, string> = {
  CREATED: '建立了任務',
  CHANGED: '修改了',
  COMMENTED: '留言',
};

/** 活動紀錄的欄位名稱 */
export const ACTIVITY_FIELD_LABELS: Partial<Record<string, string>> = {
  title: '標題',
  type: '類型',
  labelIds: '標籤',
  status: '狀態',
  priority: '優先度',
  assigneeId: '指派人',
  startDate: '開始日',
  dueDate: '到期日',
  milestoneId: '里程碑',
  sprintId: 'Sprint',
  storyPoints: '點數',
};

/** 新專案可選的顏色 */
export const DEFAULT_PROJECT_COLOR = '#1677FF';

export const PROJECT_COLORS = ['#1677FF', '#13C2C2', '#52C41A', '#FA8C16', '#EB2F96', '#722ED1', '#2F54EB', '#FAAD14'];

/** 標籤可選的顏色：專案顏色再加紅、灰 */
export const LABEL_COLORS = [...PROJECT_COLORS, '#F5222D', '#8C8C8C'];
