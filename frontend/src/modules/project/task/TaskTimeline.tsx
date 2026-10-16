import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Flex, Input, Popconfirm, Skeleton, Typography, theme } from 'antd';
import { EditOutlined, MessageOutlined, SendOutlined } from '@ant-design/icons';
import { AnimatePresence, motion } from 'motion/react';
import { useAuth } from '@/core/auth/useAuth';
import { UserAvatar, useUserDirectory, type UserDirectory } from '@/modules/basic-data';
import { formatTime } from '@/shared/lib/format';
import { activityApi } from '../activity/activity-api';
import type { TaskActivity } from '../activity/activity.types';
import { commentApi } from '../comment/comment-api';
import type { TaskComment } from '../comment/comment.types';
import { useInvalidateProjectData } from '../hooks';
import { ACTIVITY_FIELD_LABELS, TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '../labels';
import type { Milestone } from '../milestone/milestone.types';
import type { TaskPriority, TaskStatus } from './task.types';

type Entry = { kind: 'comment'; item: TaskComment } | { kind: 'activity'; item: TaskActivity };

interface TaskTimelineProps {
  taskId: number;
  milestones: readonly Milestone[];
  /** 可以留言 */
  editable: boolean;
}

function isStatus(value: string): value is TaskStatus {
  return value in TASK_STATUS_LABELS;
}

function isPriority(value: string): value is TaskPriority {
  return value in TASK_PRIORITY_LABELS;
}

/** 活動紀錄的值轉成給人看的文字 */
function describeValue(field: string | null, value: string | null, directory: UserDirectory, milestones: readonly Milestone[]): string {
  if (value === null) {
    return '（空）';
  }
  switch (field) {
    case 'status':
      return isStatus(value) ? TASK_STATUS_LABELS[value] : value;
    case 'priority':
      return isPriority(value) ? TASK_PRIORITY_LABELS[value] : value;
    case 'assigneeId':
      return directory.name(value);
    case 'milestoneId':
      return milestones.find((milestone) => String(milestone.id) === value)?.name ?? value;
    default:
      return value;
  }
}

function describeActivity(activity: TaskActivity, directory: UserDirectory, milestones: readonly Milestone[]) {
  if (activity.type === 'CREATED') {
    return '建立了任務';
  }
  const field = activity.field ?? '';
  return (
    <>
      修改了{ACTIVITY_FIELD_LABELS[field] ?? field}：
      <Typography.Text delete type="secondary">
        {describeValue(activity.field, activity.oldValue, directory, milestones)}
      </Typography.Text>
      {' → '}
      <Typography.Text strong>{describeValue(activity.field, activity.newValue, directory, milestones)}</Typography.Text>
    </>
  );
}

/** 留言與活動紀錄依時間合成一條時間軸；底部輸入留言，Ctrl + Enter 送出 */
export function TaskTimeline({ taskId, milestones, editable }: TaskTimelineProps) {
  const { user } = useAuth();
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const [draft, setDraft] = useState('');
  const [editing, setEditing] = useState<{ id: number; content: string } | null>(null);

  const comments = useQuery({ queryKey: ['task-comment', taskId], queryFn: () => commentApi.query({ taskId }) });
  const activities = useQuery({ queryKey: ['task-activity', taskId], queryFn: () => activityApi.query(taskId) });

  const entries: Entry[] = [
    ...(comments.data ?? []).map((item): Entry => ({ kind: 'comment', item })),
    // 留言本身已經列出，不重複顯示「留言」活動
    ...(activities.data ?? []).filter((item) => item.type !== 'COMMENTED').map((item): Entry => ({ kind: 'activity', item })),
  ].sort((a, b) => a.item.createTime.localeCompare(b.item.createTime));

  const directory = useUserDirectory([
    ...entries.map((entry) => entry.item.createId),
    ...entries.flatMap((entry) =>
      entry.kind === 'activity' && entry.item.field === 'assigneeId' ? [entry.item.oldValue, entry.item.newValue] : [],
    ),
  ]);

  const create = useMutation({
    mutationFn: (content: string) => commentApi.create({ taskId, content }),
    onSuccess: () => {
      setDraft('');
    },
    onSettled: invalidate,
  });
  const update = useMutation({
    mutationFn: ({ id, content }: { id: number; content: string }) => commentApi.update(id, { content }),
    onSuccess: () => {
      setEditing(null);
      void message.success('已修改留言');
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => commentApi.remove(id),
    onSettled: invalidate,
  });

  const submit = (): void => {
    const content = draft.trim();
    if (content !== '') {
      create.mutate(content);
    }
  };

  if (comments.isPending || activities.isPending) {
    return <Skeleton active avatar paragraph={{ rows: 3 }} />;
  }

  return (
    <Flex vertical gap={16}>
      <Flex vertical>
        <AnimatePresence initial={false}>
          {entries.map((entry) => {
            const actorId = entry.item.createId;
            const mine = actorId === String(user.id);
            return (
              <motion.div
                key={`${entry.kind}-${String(entry.item.id)}`}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
              >
                <Flex gap={12} style={{ paddingBottom: 16, position: 'relative' }}>
                  <UserAvatar userId={actorId} user={directory.user(actorId)} size={28} />
                  <Flex vertical gap={4} style={{ flex: 1, minWidth: 0 }}>
                    <Flex gap={8} align="baseline" wrap>
                      <Typography.Text strong>{directory.name(actorId)}</Typography.Text>
                      {entry.kind === 'activity' ? (
                        <Typography.Text type="secondary">{describeActivity(entry.item, directory, milestones)}</Typography.Text>
                      ) : (
                        <Typography.Text type="secondary">
                          <MessageOutlined /> 留言
                        </Typography.Text>
                      )}
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {formatTime(entry.item.createTime)}
                      </Typography.Text>
                    </Flex>
                    {entry.kind === 'comment' ? (
                      editing?.id === entry.item.id ? (
                        <Flex vertical gap={8}>
                          <Input.TextArea
                            autoSize={{ minRows: 2, maxRows: 8 }}
                            maxLength={2000}
                            value={editing.content}
                            onChange={(event) => {
                              setEditing({ id: entry.item.id, content: event.target.value });
                            }}
                          />
                          <Flex gap={8} justify="end">
                            <Button
                              size="small"
                              onClick={() => {
                                setEditing(null);
                              }}
                            >
                              取消
                            </Button>
                            <Button
                              size="small"
                              type="primary"
                              loading={update.isPending}
                              disabled={editing.content.trim() === ''}
                              onClick={() => {
                                update.mutate({ id: entry.item.id, content: editing.content.trim() });
                              }}
                            >
                              儲存
                            </Button>
                          </Flex>
                        </Flex>
                      ) : (
                        <div
                          style={{
                            background: token.colorFillTertiary,
                            borderRadius: token.borderRadiusLG,
                            padding: '8px 12px',
                            whiteSpace: 'pre-wrap',
                            wordBreak: 'break-word',
                          }}
                        >
                          {entry.item.content}
                          {mine && editable ? (
                            <Flex gap={4} justify="end">
                              <Button
                                type="text"
                                size="small"
                                icon={<EditOutlined />}
                                onClick={() => {
                                  setEditing({ id: entry.item.id, content: entry.item.content });
                                }}
                              />
                              <Popconfirm
                                title="刪除這則留言？"
                                okText="刪除"
                                cancelText="取消"
                                onConfirm={() => {
                                  remove.mutate(entry.item.id);
                                }}
                              >
                                <Button type="text" size="small" danger>
                                  刪除
                                </Button>
                              </Popconfirm>
                            </Flex>
                          ) : null}
                        </div>
                      )
                    ) : null}
                  </Flex>
                </Flex>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </Flex>
      {editable ? (
        <Flex vertical gap={8}>
          <Input.TextArea
            placeholder="寫下留言…（Ctrl + Enter 送出）"
            autoSize={{ minRows: 2, maxRows: 8 }}
            maxLength={2000}
            value={draft}
            onChange={(event) => {
              setDraft(event.target.value);
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
                event.preventDefault();
                submit();
              }
            }}
          />
          <Flex justify="end">
            <Button type="primary" icon={<SendOutlined />} loading={create.isPending} disabled={draft.trim() === ''} onClick={submit}>
              送出
            </Button>
          </Flex>
        </Flex>
      ) : null}
    </Flex>
  );
}
