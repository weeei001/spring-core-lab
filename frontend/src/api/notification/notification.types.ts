import type { BaseEntity } from '@/core/http/types';

export type NotificationType = 'TASK_ASSIGNED' | 'TASK_STATUS_CHANGED' | 'TASK_COMMENTED' | 'TASK_MENTIONED' | 'ANNOUNCEMENT' | 'LEAVE_SUBMITTED' | 'LEAVE_REVIEWED';

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

/** 發公告（POST /api/notification/announce，需要 notification:send） */
export interface AnnouncementRequest {
  /** 收件人 user id，最多 100 位 */
  recipientIds: string[];
  title: string;
  content?: string;
  /** true 時同時寄信給有 Email 的收件人 */
  email: boolean;
}
