import { useState, type ReactNode } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Avatar, Button, DatePicker, Flex, Input, InputNumber, Pagination, Popconfirm, Result, Select, Skeleton, Tabs, Tag, Typography } from 'antd';
import { DeleteOutlined, EyeInvisibleOutlined, EyeOutlined, LinkOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { Link } from 'react-router';
import { taskApi } from '@/api/project/task-api';
import { meetingApi } from '@/api/project/meeting-api';
import type { ClearableTaskField, Task, TaskPriority, TaskStatus, TaskType } from '@/api/project/task.types';
import { toOptions } from '@/shared/lib/format';
import { UserAvatar } from '@/components/user/UserAvatar';
import { useAuth } from '@/core/auth/useAuth';
import { PageTour } from '@/shared/ui/PageTour';
import { useInvalidateProjectData, useProjectContext, useProjectLabels, useProjectTasks } from '../hooks';
import { LabelSelect } from '../LabelSelect';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TASK_TYPE_LABELS } from '../labels';
import { DATE_FORMAT, isTaskFinished } from '../project-utils';
import { useMemberDirectory } from '../useMemberDirectory';
import { useTaskActions } from '../useTaskActions';
import { TaskAttachments } from './TaskAttachments';
import { TaskSubtasks } from './TaskSubtasks';
import { TaskTimeline } from './TaskTimeline';
import { TaskWorkLogs } from './TaskWorkLogs';
import { TASK_DRAWER_TOUR } from './tour';
import { TaskDraftBoundary, useTaskDraft } from './TaskDraftBoundary';
import { TaskRequestError } from './TaskRequestError';
import './task-detail.css';

interface TaskDetailContentProps {
  taskId: number;
  onOpen: (taskId: number) => void;
  onDeleted: (projectId: number) => void;
  navigation?: (projectId?: number) => ReactNode;
}

/** 抽屜與完整頁面共用同一份查詢、欄位、權限及更新。 */
export function TaskDetailContent({ taskId, onOpen, onDeleted, navigation }: TaskDetailContentProps) {
  const task = useQuery({
    queryKey: ['task', 'detail', taskId],
    queryFn: async () => (await taskApi.query({ ids: [taskId] }))[0] ?? null,
  });

  return (
    <Flex vertical gap={16} style={{ minWidth: 0 }}>
      {navigation?.(task.data?.projectId)}
      {task.isPending ? <Skeleton active paragraph={{ rows: 8 }} /> : task.isError ? (
        <TaskRequestError error={task.error} onRetry={() => { void task.refetch(); }} />
      ) : task.data === null ? (
        <Result status="404" title="找不到任務" subTitle="任務可能已刪除。" />
      ) : (
        <TaskDraftBoundary key={task.data.id}>
          <TaskDetailBody task={task.data} onDeleted={onDeleted} onOpen={onOpen} />
        </TaskDraftBoundary>
      )}
    </Flex>
  );
}

/** 欄位標籤 + 值，排成兩欄 */
function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Flex align="center" gap={12} style={{ minHeight: 32 }}>
      <Typography.Text type="secondary" style={{ width: 72, flexShrink: 0 }}>
        {label}
      </Typography.Text>
      <div style={{ flex: 1, minWidth: 0 }}>{children}</div>
    </Flex>
  );
}

function TaskDetailBody({ task, onDeleted, onOpen }: { task: Task; onDeleted: (projectId: number) => void; onOpen: (taskId: number) => void }) {
  const { project, members, access, loading, error, retry } = useProjectContext(task.projectId);
  const projectTasks = useProjectTasks(task.projectId);
  const labels = useProjectLabels(task.projectId);
  const { directory, options: memberOptions } = useMemberDirectory(members, [task.assigneeId, ...task.watcherIds]);
  const { update, remove, confirmStatus } = useTaskActions();
  const { user } = useAuth();
  const invalidate = useInvalidateProjectData();
  const [titleDraft, setTitleDraft] = useState<string | null>(null);
  const [descriptionDraft, setDescriptionDraft] = useState<string | null>(null);
  const [estimateDraft, setEstimateDraft] = useState<{ value: number | null } | null>(null);
  useTaskDraft('fields', (titleDraft !== null && titleDraft !== task.title)
    || (descriptionDraft !== null && descriptionDraft !== (task.description ?? ''))
    || (estimateDraft !== null && estimateDraft.value !== task.estimateHours));
  const watching = task.watcherIds.includes(String(user.id));
  const toggleWatch = useMutation({
    mutationFn: () => (watching ? taskApi.unwatch(task.id) : taskApi.watch(task.id)),
    onSettled: invalidate,
  });

  const allTasks = projectTasks.data ?? [];
  const editable = access.canEditTasks;
  const parent = task.parentId === null ? undefined : allTasks.find((other) => other.id === task.parentId);
  const save = (values: Partial<Task>, onSuccess?: () => void): void => {
    update.mutate({ id: task.id, values }, { onSuccess });
  };
  const clear = (field: ClearableTaskField): void => {
    save({ clearFields: [field] });
  };
  const changeStatus = async (status: TaskStatus): Promise<void> => {
    if (await confirmStatus(task, status, allTasks)) {
      save({ status });
    }
  };
  // 前置任務候選：同專案、不是自己、不是自己的子任務
  const predecessorOptions = allTasks
    .filter((other) => other.id !== task.id && other.parentId !== task.id)
    .map((other) => ({ value: other.id, label: `${other.code} ${other.title}` }));
  const unfinishedPredecessors = allTasks.filter(
    (other) => task.predecessorIds.includes(other.id) && !isTaskFinished(other.status),
  );

  if (loading) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }
  if (error !== null) {
    return <TaskRequestError error={error} onRetry={retry} />;
  }
  if (project === undefined) {
    return <Result status="404" title="找不到所屬專案" />;
  }
  if (!access.isMember) {
    return <Result status="403" title="無權限查看任務" />;
  }

  return (
    <Flex vertical gap={16} className="task-detail-content">
      <Flex vertical gap={4} data-tour="drawer-title">
        <Flex align="center" gap={8} wrap>
          <Typography.Text type="secondary">{task.code}</Typography.Text>
          <Link to={`/projects/${String(project.id)}`}>
            <Tag color={project.color ?? undefined}>{project.name}</Tag>
          </Link>
          {parent === undefined ? null : (
            <Typography.Link
              onClick={() => {
                onOpen(parent.id);
              }}
            >
              ↑ {parent.title}
            </Typography.Link>
          )}
        </Flex>
        {titleDraft === null ? (
          <Typography.Title
            level={4}
            editable={editable ? { editing: false, onStart: () => { setTitleDraft(task.title); } } : false}
          >
            {task.title}
          </Typography.Title>
        ) : (
          <Flex vertical gap={8}>
            <Input
              aria-label="任務標題"
              autoFocus
              maxLength={200}
              value={titleDraft}
              disabled={update.isPending}
              onChange={(event) => { setTitleDraft(event.target.value); }}
            />
            <Flex gap={8} justify="end">
              <Button onClick={() => { setTitleDraft(null); }}>取消</Button>
              <Button
                type="primary"
                loading={update.isPending}
                disabled={!editable || titleDraft.trim() === '' || titleDraft.trim() === task.title}
                onClick={() => { save({ title: titleDraft.trim() }, () => { setTitleDraft(null); }); }}
              >
                儲存
              </Button>
            </Flex>
          </Flex>
        )}
      </Flex>

      {access.closed ? <Alert type="info" showIcon title="專案已結案或取消，任務唯讀" /> : null}
      {unfinishedPredecessors.length > 0 && task.status === 'IN_PROGRESS' ? (
        <Alert
          type="warning"
          showIcon
          title={`前置任務還沒完成：${unfinishedPredecessors.map((other) => other.title).join('、')}`}
        />
      ) : null}

      <Flex vertical gap={4} data-tour="drawer-fields">
        <Field label="狀態">
          <Select<TaskStatus>
            value={task.status}
            disabled={!editable}
            options={toOptions(TASK_STATUS_LABELS)}
            style={{ width: 160 }}
            onChange={(status) => {
              void changeStatus(status);
            }}
          />
        </Field>
        <Field label="負責人">
          <Select<string>
            value={task.assigneeId ?? undefined}
            disabled={!editable}
            allowClear
            placeholder="未指派"
            options={memberOptions}
            showSearch={{ optionFilterProp: 'label' }}
            style={{ width: '100%', maxWidth: 240 }}
            onChange={(assigneeId: string | undefined) => {
              if (assigneeId === undefined) {
                clear('assigneeId');
              } else {
                save({ assigneeId });
              }
            }}
          />
        </Field>
        <Field label="日期">
          <Flex gap={8} align="center" wrap>
            <DatePicker
              placeholder="開始日"
              disabled={!editable}
              disabledDate={(date) => task.dueDate !== null && date.isAfter(task.dueDate, 'day')}
              value={task.startDate === null ? null : dayjs(task.startDate)}
              onChange={(date) => {
                if (date === null) {
                  clear('startDate');
                } else {
                  save({ startDate: date.format(DATE_FORMAT) });
                }
              }}
            />
            →
            <DatePicker
              placeholder="到期日"
              disabled={!editable}
              disabledDate={(date) => task.startDate !== null && date.isBefore(task.startDate, 'day')}
              value={task.dueDate === null ? null : dayjs(task.dueDate)}
              onChange={(date) => {
                if (date === null) {
                  clear('dueDate');
                } else {
                  save({ dueDate: date.format(DATE_FORMAT) });
                }
              }}
            />
          </Flex>
        </Field>
      </Flex>

      <details data-tour="drawer-more">
        <summary style={{ cursor: 'pointer', marginBottom: 12 }}>更多設定</summary>
        <Flex vertical gap={4}>
          <Field label="類型">
            <Select<TaskType>
              value={task.type}
              disabled={!editable}
              options={toOptions(TASK_TYPE_LABELS)}
              style={{ width: 160 }}
              onChange={(type) => {
                save({ type });
              }}
            />
          </Field>
          <Field label="標籤">
            <LabelSelect
              labels={labels.data ?? []}
              value={task.labelIds}
              disabled={!editable}
              onChange={(labelIds) => {
                save({ labelIds });
              }}
            />
          </Field>
          <Field label="優先度">
            <Select<TaskPriority>
              value={task.priority}
              disabled={!editable}
              options={toOptions(TASK_PRIORITY_LABELS)}
              style={{ width: 160 }}
              onChange={(priority) => {
                save({ priority });
              }}
            />
          </Field>
          <Field label="關注者">
            <Flex align="center" gap={8} wrap>
              {task.watcherIds.length === 0 ? (
                <Typography.Text type="secondary">沒有人關注</Typography.Text>
              ) : (
                <Avatar.Group max={{ count: 8 }}>
                  {task.watcherIds.map((id) => (
                    <UserAvatar key={id} userId={id} user={directory.user(id)} />
                  ))}
                </Avatar.Group>
              )}
              {/* eslint-disable-next-line @typescript-eslint/no-unnecessary-condition -- 保留原本操作入口的成員與結案判斷。 */}
              {access.isMember && !access.closed ? (
                <Button
                  size="small"
                  icon={watching ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                  loading={toggleWatch.isPending}
                  disabled={!access.isMember}
                  onClick={() => {
                    toggleWatch.mutate();
                  }}
                >
                  {watching ? '取消關注' : '關注'}
                </Button>
              ) : null}
            </Flex>
          </Field>
          <Field label="預估工時">
            <Flex gap={8} align="center" wrap>
              <InputNumber<number>
                aria-label="預估工時"
                value={estimateDraft === null ? task.estimateHours : estimateDraft.value}
                disabled={!editable || update.isPending}
                min={0}
                max={99999}
                step={0.5}
                precision={1}
                suffix="h"
                placeholder="未估"
                style={{ width: 160 }}
                onChange={(value) => { setEstimateDraft({ value }); }}
              />
              {estimateDraft === null ? null : (
                <>
                  <Button size="small" onClick={() => { setEstimateDraft(null); }}>取消</Button>
                  <Button
                    size="small"
                    loading={update.isPending}
                    disabled={!editable || estimateDraft.value === task.estimateHours}
                    onClick={() => {
                      save(estimateDraft.value === null ? { clearFields: ['estimateHours'] } : { estimateHours: estimateDraft.value },
                        () => { setEstimateDraft(null); });
                    }}
                  >
                    儲存
                  </Button>
                </>
              )}
            </Flex>
          </Field>
          <div data-tour="drawer-predecessors">
            <Field label="前置任務">
              <Select<number[]>
                mode="multiple"
                value={task.predecessorIds}
                disabled={!editable}
                placeholder="完成後才開始的任務"
                options={predecessorOptions}
                showSearch={{ optionFilterProp: 'label' }}
                suffix={<LinkOutlined />}
                style={{ width: '100%' }}
                onChange={(predecessorIds) => {
                  save({ predecessorIds });
                }}
              />
            </Field>
          </div>
        </Flex>
      </details>

      <Flex vertical gap={4}>
        <Typography.Text type="secondary">說明</Typography.Text>
        {descriptionDraft === null ? (
          <Typography.Paragraph
            style={{ whiteSpace: 'pre-wrap', margin: 0 }}
            type={task.description === null ? 'secondary' : undefined}
            editable={editable ? { editing: false, onStart: () => { setDescriptionDraft(task.description ?? ''); } } : false}
          >
            {task.description ?? '沒有說明'}
          </Typography.Paragraph>
        ) : (
          <Flex vertical gap={8}>
            <Input.TextArea
              aria-label="任務說明"
              autoFocus
              autoSize={{ minRows: 3, maxRows: 12 }}
              maxLength={5000}
              value={descriptionDraft}
              disabled={update.isPending}
              onChange={(event) => { setDescriptionDraft(event.target.value); }}
            />
            <Flex gap={8} justify="end">
              <Button onClick={() => { setDescriptionDraft(null); }}>取消</Button>
              <Button
                type="primary"
                loading={update.isPending}
                disabled={!editable || descriptionDraft.trim() === (task.description ?? '')}
                onClick={() => {
                  const description = descriptionDraft.trim();
                  save(description === '' ? { clearFields: ['description'] } : { description }, () => { setDescriptionDraft(null); });
                }}
              >
                儲存
              </Button>
            </Flex>
          </Flex>
        )}
      </Flex>

      <TaskMeetings projectId={task.projectId} taskId={task.id} />

      <div data-tour="drawer-tabs" data-tour-inner=".ant-tabs-nav">
        <Tabs
          defaultActiveKey="activity"
          items={[
            ...(task.parentId === null
              ? [
                  {
                    key: 'subtasks',
                    label: `子任務${task.subtaskCount === undefined ? '' : `（${String(task.subtaskCount)}）`}`,
                    children: (
                      <TaskSubtasks parent={task} allTasks={allTasks} directory={directory} editable={editable} onOpen={onOpen} />
                    ),
                  },
                ]
              : []),
            {
              key: 'activity',
              label: '留言與活動',
              children: <TaskTimeline taskId={task.id} labels={labels.data ?? []} members={memberOptions} editable={editable} />,
            },
            {
              key: 'attachments',
              label: '附件',
              children: <TaskAttachments taskId={task.id} editable={editable} />,
            },
            {
              key: 'work-log',
              label: '工時',
              children: <TaskWorkLogs task={task} editable={access.canLogWork} />,
            },
          ]}
        />
      </div>

      {editable ? (
        <Flex justify="end">
          <Popconfirm
            title="刪除這個任務？"
            description="有子任務時無法刪除；其他任務的前置任務會一併移除它。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(task.id, { onSuccess: () => { onDeleted(task.projectId); } });
            }}
          >
            <Button danger icon={<DeleteOutlined />}>
              刪除任務
            </Button>
          </Popconfirm>
        </Flex>
      ) : null}
      {/* 抽屜與完整頁面使用相同的操作導覽。 */}
      <PageTour id="task-drawer" steps={TASK_DRAWER_TOUR} />
    </Flex>
  );
}

function TaskMeetings({ projectId, taskId }: { projectId: number; taskId: number }) {
  const [page, setPage] = useState(1);
  const meetings = useQuery({
    queryKey: ['meeting', 'task', projectId, taskId, page],
    queryFn: () => meetingApi.queryPage({ projectId, taskId, page, size: 20 }),
    staleTime: 0,
  });
  return (
    <Flex vertical gap={8}>
      <Typography.Text type="secondary">來源會議</Typography.Text>
      {meetings.isPending ? <Skeleton active paragraph={{ rows: 1 }} /> : meetings.isError ? (
        <Alert type="error" title={meetings.error.message} action={<Button onClick={() => { void meetings.refetch(); }}>重試</Button>} />
      ) : meetings.data.items.length === 0 ? <Typography.Text type="secondary">沒有連結會議</Typography.Text> : <>
        {meetings.data.items.map((meeting) => <Link key={meeting.id} to={`/projects/${String(projectId)}/meetings/${String(meeting.id)}`}>{meeting.title}</Link>)}
        {meetings.data.total > 20 ? <Pagination current={page} pageSize={20} total={meetings.data.total} showSizeChanger={false} onChange={setPage} /> : null}
      </>}
    </Flex>
  );
}
