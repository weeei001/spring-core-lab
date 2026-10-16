import { http, send } from '@/core/http/client';
import type { PageQuery, PageResponse, Result } from '@/core/http/types';
import type { Notification, NotificationQuery } from './notification.types';

/** 只查得到、只能標記自己的通知（web-server 依登入者限縮） */
export const notificationApi = {
  queryPage: (query: NotificationQuery & PageQuery) =>
    send(http.post<Result<PageResponse<Notification>>>('/api/notification/queryPage', query)),
  unreadCount: () => send(http.get<Result<number>>('/api/notification/unread-count')),
  markRead: (id: number) => send(http.post<Result<Notification>>(`/api/notification/${String(id)}/read`)),
  /** 回傳這次標記的筆數 */
  markAllRead: () => send(http.post<Result<number>>('/api/notification/read-all')),
};
