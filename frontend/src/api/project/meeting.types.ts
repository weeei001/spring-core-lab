import type { BaseEntity } from '@/core/http/types';

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

export interface MeetingQuery {
  projectId: number;
  ids?: number[];
  taskId?: number;
  fromHeldAt?: string;
  toHeldAt?: string;
}
