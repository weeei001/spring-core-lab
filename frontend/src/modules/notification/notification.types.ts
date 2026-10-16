import type { BaseEntity } from '@/core/http/types';

export type NotificationType = 'TASK_ASSIGNED' | 'TASK_STATUS_CHANGED' | 'TASK_COMMENTED' | 'ANNOUNCEMENT';

/** 站內通知（後端 Notification）；觸發的人是 createId，系統觸發為 system */
export interface Notification extends BaseEntity {
  recipientId: string;
  type: NotificationType;
  title: string;
  content: string | null;
  /** 點通知要前往的站內路徑，例如 /projects/1?tab=tasks&task=2 */
  link: string | null;
  /** 已讀時間；null 為未讀 */
  readTime: string | null;
}

export interface NotificationQuery {
  type?: NotificationType;
  /** true 只查未讀 */
  unread?: boolean;
}
