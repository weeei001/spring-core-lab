import { useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Badge, Button, Empty, Flex, Popover, Skeleton, Typography, theme } from 'antd';
import { BellOutlined, CalendarOutlined, CheckCircleOutlined, CommentOutlined, MessageOutlined, NotificationOutlined, SwapOutlined, UserAddOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { notificationApi } from '@/api/notification/notification-api';
import type { Notification, NotificationType } from '@/api/notification/notification.types';
import { ApiError } from '@/core/http/client';
import { formatTime } from '@/shared/lib/format';

/** 多久重查一次未讀數 */
const POLL_INTERVAL = 30_000;
/** 下拉清單顯示最新幾筆 */
const LIST_SIZE = 20;

const TYPE_ICONS: Record<NotificationType, ReactNode> = {
  TASK_ASSIGNED: <UserAddOutlined />,
  TASK_STATUS_CHANGED: <SwapOutlined />,
  TASK_COMMENTED: <MessageOutlined />,
  TASK_MENTIONED: <CommentOutlined />,
  ANNOUNCEMENT: <NotificationOutlined />,
  LEAVE_SUBMITTED: <CalendarOutlined />,
  LEAVE_REVIEWED: <CheckCircleOutlined />,
  LEAVE_CANCELLED: <CalendarOutlined />,
  TASK_DAILY_DIGEST: <BellOutlined />,
};

/**
 * 右上角的通知鈴鐺：定期查未讀數；點開列出最新通知，點一則標記已讀並前往連結。
 * notification-service 沒啟動時只是不顯示數字，不每 30 秒跳錯誤訊息；401 仍交給 query-client 處理登入。
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const unread = useQuery({
    queryKey: ['notification', 'unread-count'],
    queryFn: async () => {
      try {
        return await notificationApi.unreadCount();
      } catch (error: unknown) {
        if (error instanceof ApiError && error.status === 401) {
          throw error;
        }
        return null;
      }
    },
    refetchInterval: POLL_INTERVAL,
  });

  const list = useQuery({
    queryKey: ['notification', 'latest'],
    queryFn: () =>
      notificationApi.queryPage({ page: 1, size: LIST_SIZE, sort: [{ field: 'createTime', direction: 'desc' }] }),
    enabled: open,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['notification'] });
  const markRead = useMutation({ mutationFn: notificationApi.markRead, onSuccess: refresh });
  const markAllRead = useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: refresh });

  const openNotification = (notification: Notification) => {
    if (notification.readTime === null) {
      markRead.mutate(notification.id);
    }
    if (notification.link !== null) {
      setOpen(false);
      void navigate(notification.link);
    }
  };

  const items = list.data?.items ?? [];
  const content = (
    <Flex vertical style={{ width: 360 }}>
      <Flex justify="space-between" align="center" style={{ paddingBottom: 8 }}>
        <Typography.Text strong>通知</Typography.Text>
        <Button
          type="link"
          size="small"
          disabled={!unread.data}
          loading={markAllRead.isPending}
          onClick={() => {
            markAllRead.mutate();
          }}
        >
          全部標為已讀
        </Button>
      </Flex>
      <div style={{ maxHeight: 420, overflowY: 'auto' }}>
        {list.isPending ? (
          <Skeleton active paragraph={{ rows: 4 }} />
        ) : items.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="沒有通知" />
        ) : (
          items.map((notification) => (
            <NotificationItem key={notification.id} notification={notification} onOpen={openNotification} />
          ))
        )}
      </div>
    </Flex>
  );

  return (
    <Popover content={content} trigger="click" placement="bottomRight" open={open} onOpenChange={setOpen}>
      <Badge count={unread.data ?? 0} size="small" overflowCount={99}>
        <Button type="text" aria-label="通知" icon={<BellOutlined />} />
      </Badge>
    </Popover>
  );
}

function NotificationItem({
  notification,
  onOpen,
}: {
  notification: Notification;
  onOpen: (notification: Notification) => void;
}) {
  const { token } = theme.useToken();
  const isUnread = notification.readTime === null;
  return (
    <Flex
      gap={12}
      role="button"
      tabIndex={0}
      onClick={() => {
        onOpen(notification);
      }}
      onKeyDown={(event) => {
        if (event.key === 'Enter') {
          onOpen(notification);
        }
      }}
      style={{
        padding: '10px 8px',
        cursor: 'pointer',
        borderRadius: token.borderRadius,
        background: isUnread ? token.colorPrimaryBg : undefined,
        marginBottom: 4,
      }}
    >
      <span style={{ color: isUnread ? token.colorPrimary : token.colorTextTertiary, fontSize: 16 }}>
        {TYPE_ICONS[notification.type]}
      </span>
      <Flex vertical style={{ minWidth: 0, flex: 1 }}>
        <Typography.Text strong={isUnread} ellipsis>
          {notification.title}
        </Typography.Text>
        {notification.content !== null && (
          <Typography.Paragraph
            type="secondary"
            ellipsis={{ rows: 2 }}
            style={{ marginBottom: 0, whiteSpace: 'pre-line', fontSize: token.fontSizeSM }}
          >
            {notification.content}
          </Typography.Paragraph>
        )}
        <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
          {formatTime(notification.createTime)}
        </Typography.Text>
      </Flex>
    </Flex>
  );
}
