import type { ReactNode } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Alert, Avatar, Button, DatePicker, Drawer, Flex, InputNumber, Popconfirm, Result, Select, Skeleton, Tabs, Tag, Typography } from 'antd';
import { DeleteOutlined, EyeInvisibleOutlined, EyeOutlined, LinkOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { Link } from 'react-router';
import { taskApi } from '@/api/project/task-api';
import type { ClearableTaskField, Task, TaskPriority, TaskStatus, TaskType } from '@/api/project/task.types';
import { toOptions } from '@/shared/lib/format';
import { UserAvatar } from '@/components/user/UserAvatar';
import { useAuth } from '@/core/auth/useAuth';
import { PageTour } from '@/shared/ui/PageTour';
import { useInvalidateProjectData, useProjectContext, useProjectLabels, useProjectMilestones, useProjectTasks } from '../hooks';
import { LabelSelect } from '../LabelSelect';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS, TASK_TYPE_LABELS } from '../labels';
import { DATE_FORMAT, isTaskFinished } from '../project-utils';
import { useMemberDirectory } from '../useMemberDirectory';
import { useTaskActions } from '../useTaskActions';
import { TaskSubtasks } from './TaskSubtasks';
import { TaskTimeline } from './TaskTimeline';
import { TaskWorkLogs } from './TaskWorkLogs';
import { TASK_DRAWER_TOUR } from './tour';

interface TaskDrawerProps {
  /** undefined 表示關閉 */
  taskId: number | undefined;
  onClose: () => void;
  /** 開啟另一個任務（子任務、父任務、前置任務） */
  onOpen: (taskId: number) => void;
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

function TaskDrawerBody({ task, onClose, onOpen }: { task: Task; onClose: () => void; onOpen: (taskId: number) => void }) {
  const { project, members, access } = useProjectContext(task.projectId);
  const projectTasks = useProjectTasks(task.projectId);
  const milestones = useProjectMilestones(task.projectId);
  const labels = useProjectLabels(task.projectId);
  const { directory, options: memberOptions } = useMemberDirectory(members, [task.assigneeId, ...task.watcherIds]);
  const { update, remove, confirmStatus } = useTaskActions();
  const { user } = useAuth();
  const invalidate = useInvalidateProjectData();
  const watching = task.watcherIds.includes(String(user.id));
  const toggleWatch = useMutation({
    mutationFn: () => (watching ? taskApi.unwatch(task.id) : taskApi.watch(task.id)),
    onSettled: invalidate,
  });

  const allTasks = projectTasks.data ?? [];
  const editable = access.canEditTasks;
  const parent = task.parentId === null ? undefined : allTasks.find((other) => other.id === task.parentId);
  const save = (values: Partial<Task>): void => {
    update.mutate({ id: task.id, values });
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

  return (
    <Flex vertical gap={16}>
      <Flex vertical gap={4} data-tour="drawer-title">
        <Flex align="center" gap={8} wrap>
          <Typography.Text type="secondary">{task.code}</Typography.Text>
          {project === undefined ? null : (
            <Link to={`/projects/${String(project.id)}`} onClick={onClose}>
              <Tag color={project.color ?? undefined}>{project.name}</Tag>
            </Link>
          )}
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
        <Typography.Title
          level={4}
          editable={
            editable
              ? {
                  onChange: (title) => {
                    const trimmed = title.trim();
                    if (trimmed !== '' && trimmed !== task.title) {
                      save({ title: trimmed });
                    }
                  },
                  maxLength: 200,
                }
              : false
          }
        >
          {task.title}
        </Typography.Title>
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
        <Field label="指派人">
          <Select<string>
            value={task.assigneeId ?? undefined}
            disabled={!editable}
            allowClear
            placeholder="未指派"
            options={memberOptions}
            showSearch={{ optionFilterProp: 'label' }}
            style={{ width: 240 }}
            onChange={(assigneeId: string | undefined) => {
              if (assigneeId === undefined) {
                clear('assigneeId');
              } else {
                save({ assigneeId });
              }
            }}
          />
        </Field>
        <Field label="里程碑">
          <Select<number>
            value={task.milestoneId ?? undefined}
            disabled={!editable}
            allowClear
            placeholder="不屬於里程碑"
            options={(milestones.data ?? []).map((milestone) => ({ value: milestone.id, label: milestone.name }))}
            style={{ width: 240 }}
            onChange={(milestoneId: number | undefined) => {
              if (milestoneId === undefined) {
                clear('milestoneId');
              } else {
                save({ milestoneId });
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
            {access.isMember && !access.closed ? (
              <Button
                size="small"
                icon={watching ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                loading={toggleWatch.isPending}
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
          <InputNumber
            key={`estimate-${String(task.estimateHours)}`}
            defaultValue={task.estimateHours ?? undefined}
            disabled={!editable}
            min={0}
            max={99999}
            step={0.5}
            precision={1}
            suffix="h"
            placeholder="未估"
            style={{ width: 160 }}
            onBlur={(event) => {
              // 這裡比 InputNumber 自己的範圍修正早執行，拿到的是原始輸入，要自己檢查
              const text = event.target.value.trim();
              const hours = text === '' ? null : Number(text);
              if (hours === task.estimateHours || (hours !== null && (Number.isNaN(hours) || hours < 0 || hours > 99999))) {
                return;
              }
              if (hours === null) {
                clear('estimateHours');
              } else {
                save({ estimateHours: hours });
              }
            }}
          />
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

      <Flex vertical gap={4}>
        <Typography.Text type="secondary">說明</Typography.Text>
        <Typography.Paragraph
          style={{ whiteSpace: 'pre-wrap', margin: 0 }}
          type={task.description === null ? 'secondary' : undefined}
          editable={
            editable
              ? {
                  text: task.description ?? '',
                  autoSize: { minRows: 3, maxRows: 12 },
                  maxLength: 5000,
                  onChange: (text) => {
                    const trimmed = text.trim();
                    if (trimmed === (task.description ?? '')) {
                      return;
                    }
                    if (trimmed === '') {
                      clear('description');
                    } else {
                      save({ description: trimmed });
                    }
                  },
                }
              : false
          }
        >
          {task.description ?? '沒有說明'}
        </Typography.Paragraph>
      </Flex>

      <div data-tour="drawer-tabs" data-tour-inner=".ant-tabs-nav">
        <Tabs
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
              children: <TaskTimeline taskId={task.id} milestones={milestones.data ?? []} labels={labels.data ?? []} members={memberOptions} editable={editable} />,
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
              remove.mutate(task.id, { onSuccess: onClose });
            }}
          >
            <Button danger icon={<DeleteOutlined />}>
              刪除任務
            </Button>
          </Popconfirm>
        </Flex>
      ) : null}
      {/* 第一次打開任務抽屜時播放；抽屜開著時按「?」重看的是這個 */}
      <PageTour id="task-drawer" steps={TASK_DRAWER_TOUR} />
    </Flex>
  );
}

/** 任務詳情抽屜：欄位直接改、子任務、留言與活動、工時；任何頁面都能開（網址 ?task=id） */
export function TaskDrawer({ taskId, onClose, onOpen }: TaskDrawerProps) {
  const task = useQuery({
    queryKey: ['task', 'detail', taskId],
    queryFn: async () => (taskId === undefined ? null : ((await taskApi.query({ ids: [taskId] }))[0] ?? null)),
    enabled: taskId !== undefined,
  });

  return (
    <Drawer open={taskId !== undefined} onClose={onClose} size={720} destroyOnHidden title="任務">
      {task.isPending ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : task.data == null ? (
        <Result status="404" title="找不到任務" subTitle="任務可能已刪除，或你不是這個專案的成員。" />
      ) : (
        // 換任務時重建，留言草稿、工時表單、分頁才不會帶到下一個任務
        <TaskDrawerBody key={task.data.id} task={task.data} onClose={onClose} onOpen={onOpen} />
      )}
    </Drawer>
  );
}
