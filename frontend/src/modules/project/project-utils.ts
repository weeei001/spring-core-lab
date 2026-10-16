import dayjs, { type Dayjs } from 'dayjs';
import type { ProjectStatus } from './project/project.types';
import type { Task, TaskStatus } from './task/task.types';

export const DATE_FORMAT = 'YYYY-MM-DD';

export function todayText(): string {
  return dayjs().format(DATE_FORMAT);
}

/** 完成或取消：不算逾期 */
export function isTaskFinished(status: TaskStatus): boolean {
  return status === 'DONE' || status === 'CANCELLED';
}

/** 結案或取消：專案唯讀 */
export function isProjectClosed(status: ProjectStatus): boolean {
  return status === 'DONE' || status === 'CANCELLED';
}

export function isOverdue(task: Pick<Task, 'dueDate' | 'status'>): boolean {
  return task.dueDate !== null && !isTaskFinished(task.status) && task.dueDate < todayText();
}

/** 0–100 的整數；total 為 0 時回 0 */
export function percent(done: number | undefined, total: number | undefined): number {
  if (total === undefined || total === 0) {
    return 0;
  }
  return Math.round(((done ?? 0) / total) * 100);
}

/** 該週的星期一 */
export function weekStart(date: Dayjs): Dayjs {
  return date.subtract((date.day() + 6) % 7, 'day').startOf('day');
}

/** 到期日的相對說明，例如「今天」「3 天後」「逾期 2 天」 */
export function dueText(dueDate: string): string {
  const days = dayjs(dueDate).diff(dayjs().startOf('day'), 'day');
  if (days === 0) {
    return '今天';
  }
  if (days === 1) {
    return '明天';
  }
  return days > 0 ? `${String(days)} 天後` : `逾期 ${String(-days)} 天`;
}

/** 小時數顯示，例如 3.5 → 3.5h、4 → 4h */
export function hoursText(hours: number): string {
  return `${String(Math.round(hours * 10) / 10)}h`;
}

/** 有非空白的文字；清空後的說明存成空字串，顯示時要和 null 一樣處理 */
export function hasText(value: string | null | undefined): value is string {
  return value != null && value.trim() !== '';
}
