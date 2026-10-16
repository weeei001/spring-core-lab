import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Card, DatePicker, Empty, Flex, Form, Input, Modal, Popconfirm, Progress, Select, Skeleton, Tag, Typography } from 'antd';
import { CheckOutlined, DeleteOutlined, EditOutlined, PlayCircleOutlined, PlusOutlined, RollbackOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { sprintApi } from '@/api/project/sprint-api';
import type { Sprint, SprintBurndown } from '@/api/project/sprint.types';
import type { Task } from '@/api/project/task.types';
import { TaskStatusTag, TaskTypeTag } from '@/components/project/Tags';
import { useInvalidateProjectData, useProjectSprints, useProjectTasks, type ProjectAccess } from '@/components/project/hooks';
import { SPRINT_STATUS_COLORS, SPRINT_STATUS_LABELS, STORY_POINT_OPTIONS } from '@/components/project/labels';
import { DATE_FORMAT, hasText, isTaskFinished, percent, todayText } from '@/components/project/project-utils';
import { useTaskActions } from '@/components/project/useTaskActions';
import { UserAvatar } from '@/components/user/UserAvatar';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { BurndownChart } from '@/shared/ui/BurndownChart';
import { FormModal } from '@/shared/ui/FormModal';

interface SprintFormValues {
  name: string;
  goal?: string;
  range: [Dayjs, Dayjs];
}

type Editing = { mode: 'create' } | { mode: 'edit'; sprint: Sprint } | null;

/** 結束 Sprint 時未完成任務的去處；BACKLOG 是特殊值 */
const BACKLOG = 0;

interface SprintTabProps {
  projectId: number;
  access: ProjectAccess;
  directory: UserDirectory;
  onOpen: (taskId: number) => void;
}

/**
 * 燃盡圖的資料：從開始日到結束日的每一天，取當天（或之前最近一天）的紀錄；今天之後為 null。
 * 有填故事點數就用點數，都沒填就用任務數。
 */
function buildBurndown(sprint: Sprint, rows: readonly SprintBurndown[]) {
  const usePoints = rows.some((row) => row.totalPoints > 0) || (sprint.totalPoints ?? 0) > 0;
  const days: string[] = [];
  for (let day = dayjs(sprint.startDate); !day.isAfter(sprint.endDate, 'day'); day = day.add(1, 'day')) {
    days.push(day.format(DATE_FORMAT));
  }
  const today = todayText();
  let index = 0;
  let current: SprintBurndown | undefined;
  const remaining = days.map((day) => {
    for (let next = rows[index]; next !== undefined && next.day <= day; next = rows[index]) {
      current = next;
      index++;
    }
    if (day > today || current === undefined) {
      return null;
    }
    return usePoints ? current.totalPoints - current.donePoints : current.taskCount - current.doneTaskCount;
  });
  const first = rows[0];
  const total = first === undefined ? 0 : usePoints ? first.totalPoints : first.taskCount;
  return { days, remaining, total, unit: usePoints ? '點' : '個任務' };
}

function SprintBurndownCard({ sprint }: { sprint: Sprint }) {
  const burndown = useQuery({
    queryKey: ['sprint', 'burndown', sprint.id],
    queryFn: () => sprintApi.burndown(sprint.id),
  });
  if (burndown.isPending) {
    return <Skeleton active paragraph={{ rows: 4 }} />;
  }
  const data = buildBurndown(sprint, burndown.data ?? []);
  return <BurndownChart days={data.days} remaining={data.remaining} total={data.total} unit={data.unit} />;
}

interface TaskRowsProps {
  tasks: readonly Task[];
  directory: UserDirectory;
  editable: boolean;
  /** 可以排入的 Sprint（Backlog 列表用） */
  targets?: readonly Sprint[];
  /** 顯示「移回 Backlog」按鈕 */
  removable?: boolean;
  onOpen: (taskId: number) => void;
}

/** Sprint 或 Backlog 裡的任務列表：類型、編號、標題、狀態、指派人、點數 */
function TaskRows({ tasks, directory, editable, targets, removable = false, onOpen }: TaskRowsProps) {
  const { update } = useTaskActions();
  if (tasks.length === 0) {
    return <Typography.Text type="secondary">沒有任務</Typography.Text>;
  }
  return (
    <Flex vertical>
      {tasks.map((task) => (
        <Flex key={task.id} align="center" gap={8} wrap style={{ padding: '6px 0', borderBottom: '1px solid rgba(0,0,0,0.04)' }}>
          <TaskTypeTag type={task.type} iconOnly />
          <Typography.Text type="secondary" style={{ width: 90 }}>
            {task.code}
          </Typography.Text>
          <Typography.Link
            ellipsis
            style={{ flex: 1, minWidth: 160 }}
            delete={task.status === 'CANCELLED'}
            onClick={() => {
              onOpen(task.id);
            }}
          >
            {task.title}
          </Typography.Link>
          <TaskStatusTag status={task.status} />
          {task.assigneeId === null ? null : <UserAvatar userId={task.assigneeId} user={directory.user(task.assigneeId)} />}
          <Select<number>
            size="small"
            allowClear
            disabled={!editable}
            placeholder="點數"
            value={task.storyPoints ?? undefined}
            options={STORY_POINT_OPTIONS.map((points) => ({ value: points, label: `${String(points)} 點` }))}
            style={{ width: 84 }}
            onChange={(storyPoints: number | undefined) => {
              update.mutate({
                id: task.id,
                values: storyPoints === undefined ? { clearFields: ['storyPoints'] } : { storyPoints },
              });
            }}
          />
          {editable && targets !== undefined ? (
            <Select<number>
              size="small"
              placeholder="排入 Sprint"
              value={undefined}
              disabled={targets.length === 0}
              options={targets.map((sprint) => ({ value: sprint.id, label: sprint.name }))}
              style={{ width: 140 }}
              onChange={(sprintId) => {
                update.mutate({ id: task.id, values: { sprintId } });
              }}
            />
          ) : null}
          {editable && removable ? (
            <Button
              size="small"
              type="text"
              icon={<RollbackOutlined />}
              title="移回 Backlog"
              onClick={() => {
                update.mutate({ id: task.id, values: { clearFields: ['sprintId'] } });
              }}
            />
          ) : null}
        </Flex>
      ))}
    </Flex>
  );
}

/** Sprint 規劃：進行中的 Sprint 與燃盡圖、規劃中的 Sprint、Backlog；經理維護 Sprint，成員可以把任務排入或移出 */
export function SprintTab({ projectId, access, directory, onOpen }: SprintTabProps) {
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const sprints = useProjectSprints(projectId);
  const tasks = useProjectTasks(projectId);
  const [editing, setEditing] = useState<Editing>(null);
  const [closing, setClosing] = useState<Sprint | null>(null);
  const [moveTo, setMoveTo] = useState<number>(BACKLOG);

  const save = useMutation({
    mutationFn: ({ id, values }: { id?: number; values: Partial<Sprint> }) =>
      id === undefined ? sprintApi.create({ ...values, projectId }) : sprintApi.update(id, values),
    onSuccess: () => {
      void message.success('已儲存');
      setEditing(null);
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => sprintApi.remove(id),
    onSuccess: () => {
      void message.success('已刪除 Sprint，任務已移回 Backlog');
    },
    onSettled: invalidate,
  });
  const start = useMutation({
    mutationFn: (id: number) => sprintApi.start(id),
    onSuccess: () => {
      void message.success('Sprint 已開始');
    },
    onSettled: invalidate,
  });
  const close = useMutation({
    mutationFn: ({ id, target }: { id: number; target: number }) => sprintApi.close(id, target === BACKLOG ? undefined : target),
    onSuccess: () => {
      void message.success('Sprint 已結束');
      setClosing(null);
    },
    onSettled: invalidate,
  });

  if (sprints.isPending || tasks.isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  const all = sprints.data ?? [];
  const topLevel = (tasks.data ?? []).filter((task) => task.parentId === null);
  const active = all.find((sprint) => sprint.status === 'ACTIVE');
  const planned = all.filter((sprint) => sprint.status === 'PLANNED');
  const closed = all.filter((sprint) => sprint.status === 'CLOSED').reverse();
  const open = all.filter((sprint) => sprint.status !== 'CLOSED');
  const backlog = topLevel.filter((task) => task.sprintId === null && !isTaskFinished(task.status));
  const tasksOf = (sprint: Sprint): Task[] => topLevel.filter((task) => task.sprintId === sprint.id);

  const sprintActions = (sprint: Sprint) =>
    access.canManage && sprint.status !== 'CLOSED' ? (
      <Flex gap={4}>
        {sprint.status === 'PLANNED' ? (
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            disabled={active !== undefined}
            title={active === undefined ? undefined : '已有進行中的 Sprint'}
            loading={start.isPending}
            onClick={() => {
              start.mutate(sprint.id);
            }}
          >
            開始
          </Button>
        ) : (
          <Button
            size="small"
            type="primary"
            icon={<CheckOutlined />}
            onClick={() => {
              setMoveTo(planned[0]?.id ?? BACKLOG);
              setClosing(sprint);
            }}
          >
            結束 Sprint
          </Button>
        )}
        <Button
          type="text"
          size="small"
          icon={<EditOutlined />}
          onClick={() => {
            setEditing({ mode: 'edit', sprint });
          }}
        />
        {sprint.status === 'PLANNED' ? (
          <Popconfirm
            title={`刪除「${sprint.name}」？`}
            description="排入的任務會移回 Backlog。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(sprint.id);
            }}
          >
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        ) : null}
      </Flex>
    ) : null;

  const sprintTitle = (sprint: Sprint) => (
    <Flex align="center" gap={8} wrap>
      <Typography.Text strong>{sprint.name}</Typography.Text>
      <Tag color={SPRINT_STATUS_COLORS[sprint.status]}>{SPRINT_STATUS_LABELS[sprint.status]}</Tag>
      <Typography.Text type="secondary" style={{ fontWeight: 'normal' }}>
        {sprint.startDate} ～ {sprint.endDate}
      </Typography.Text>
    </Flex>
  );

  const progress = (sprint: Sprint) => (
    <Flex align="center" gap={12}>
      <Progress percent={percent(sprint.donePoints, sprint.totalPoints)} style={{ flex: 1, margin: 0 }} />
      <Typography.Text type="secondary" style={{ whiteSpace: 'nowrap' }}>
        {String(sprint.donePoints ?? 0)} / {String(sprint.totalPoints ?? 0)} 點 · {String(sprint.doneTaskCount ?? 0)} /{' '}
        {String(sprint.taskCount ?? 0)} 個任務
      </Typography.Text>
    </Flex>
  );

  return (
    <Flex vertical gap={16}>
      {access.canManage ? (
        <Flex justify="end">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing({ mode: 'create' });
            }}
          >
            新增 Sprint
          </Button>
        </Flex>
      ) : null}

      {active === undefined ? null : (
        <Card title={sprintTitle(active)} extra={sprintActions(active)}>
          <Flex vertical gap={16}>
            {hasText(active.goal) ? <Typography.Paragraph style={{ margin: 0 }}>目標：{active.goal}</Typography.Paragraph> : null}
            {progress(active)}
            <SprintBurndownCard sprint={active} />
            <TaskRows tasks={tasksOf(active)} directory={directory} editable={access.canEditTasks} removable onOpen={onOpen} />
          </Flex>
        </Card>
      )}

      {planned.map((sprint) => (
        <Card key={sprint.id} size="small" title={sprintTitle(sprint)} extra={sprintActions(sprint)}>
          <Flex vertical gap={8}>
            {hasText(sprint.goal) ? <Typography.Text type="secondary">目標：{sprint.goal}</Typography.Text> : null}
            <Typography.Text type="secondary">
              {String(sprint.taskCount ?? 0)} 個任務 · {String(sprint.totalPoints ?? 0)} 點
            </Typography.Text>
            <TaskRows tasks={tasksOf(sprint)} directory={directory} editable={access.canEditTasks} removable onOpen={onOpen} />
          </Flex>
        </Card>
      ))}

      <Card size="small" title={`Backlog（${String(backlog.length)}）`}>
        {open.length === 0 && access.canManage ? (
          <Typography.Paragraph type="secondary">先新增一個 Sprint，就可以把 Backlog 的任務排進去。</Typography.Paragraph>
        ) : null}
        <TaskRows tasks={backlog} directory={directory} editable={access.canEditTasks} targets={open} onOpen={onOpen} />
      </Card>

      {closed.length === 0 ? null : (
        <Card size="small" title="已結束的 Sprint">
          <Flex vertical gap={12}>
            {closed.map((sprint) => (
              <Flex key={sprint.id} vertical gap={4}>
                {sprintTitle(sprint)}
                {progress(sprint)}
              </Flex>
            ))}
          </Flex>
        </Card>
      )}

      {all.length === 0 && !access.canManage ? <Empty description="還沒有 Sprint" /> : null}

      <Modal
        open={closing !== null}
        title={`結束「${closing?.name ?? ''}」`}
        okText="結束 Sprint"
        cancelText="取消"
        confirmLoading={close.isPending}
        onCancel={() => {
          setClosing(null);
        }}
        onOk={() => {
          if (closing !== null) {
            close.mutate({ id: closing.id, target: moveTo });
          }
        }}
        destroyOnHidden
      >
        <Flex vertical gap={8}>
          <Typography.Text>
            還有 {String(closing === null ? 0 : tasksOf(closing).filter((task) => !isTaskFinished(task.status)).length)} 個未完成的任務，要移到：
          </Typography.Text>
          <Select<number>
            value={moveTo}
            options={[{ value: BACKLOG, label: 'Backlog' }, ...planned.map((sprint) => ({ value: sprint.id, label: sprint.name }))]}
            onChange={setMoveTo}
          />
        </Flex>
      </Modal>

      <FormModal<SprintFormValues>
        open={editing !== null}
        title={editing?.mode === 'edit' ? '修改 Sprint' : '新增 Sprint'}
        initialValues={
          editing?.mode === 'edit'
            ? {
                name: editing.sprint.name,
                goal: editing.sprint.goal ?? undefined,
                range: [dayjs(editing.sprint.startDate), dayjs(editing.sprint.endDate)],
              }
            : {
                name: `Sprint ${String(all.length + 1)}`,
                // 預設接在最後一個 Sprint 之後，為期兩週
                range: (() => {
                  const last = all.at(-1);
                  const startDay = last === undefined ? dayjs() : dayjs(last.endDate).add(1, 'day');
                  return [startDay, startDay.add(13, 'day')];
                })(),
              }
        }
        saving={save.isPending}
        onCancel={() => {
          setEditing(null);
        }}
        onSubmit={(values) => {
          save.mutate({
            id: editing?.mode === 'edit' ? editing.sprint.id : undefined,
            values: {
              name: values.name.trim(),
              // 清空目標時存成空字串（後端更新時 null 代表不改）
              goal: values.goal?.trim() ?? '',
              startDate: values.range[0].format(DATE_FORMAT),
              endDate: values.range[1].format(DATE_FORMAT),
            },
          });
        }}
      >
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
          <Input autoFocus />
        </Form.Item>
        <Form.Item name="range" label="期間" rules={[{ required: true, message: '請選擇期間' }]}>
          <DatePicker.RangePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="goal" label="目標" rules={[{ max: 1000 }]}>
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} placeholder="這個 Sprint 要達成什麼" />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
