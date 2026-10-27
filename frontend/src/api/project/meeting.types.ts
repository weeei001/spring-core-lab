import type { BaseEntity } from '@/core/http/types';
import type { TaskStatus } from './task.types';

export interface Meeting extends BaseEntity {
  projectId: number;
  title: string;
  /** Asia/Taipei 的 ISO local datetime，不能含 Z 或 offset。 */
  heldAt: string;
  participants: string;
  minutes: string;
  taskIds: number[];
}

export type MeetingInput = Pick<Meeting, 'title' | 'heldAt' | 'participants' | 'minutes'>;

export interface MeetingTaskView {
  taskId: number;
  deleted: boolean;
  code: string | null;
  title: string | null;
  assigneeId: string | null;
  dueDate: string | null;
  status: TaskStatus | null;
}

export interface MeetingTaskInput {
  title: string;
  assigneeId?: string;
  dueDate?: string;
}

export interface MeetingQuery {
  projectId: number;
  ids?: number[];
  taskId?: number;
  fromHeldAt?: string;
  toHeldAt?: string;
}
