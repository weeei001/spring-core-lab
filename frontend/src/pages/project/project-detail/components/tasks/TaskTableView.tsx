import { Flex, Select, Table, Tag, Tooltip, Typography, type TableColumnsType } from 'antd';
import { BranchesOutlined, LinkOutlined } from '@ant-design/icons';
import type { Milestone } from '@/api/project/milestone.types';
import type { Task, TaskStatus } from '@/api/project/task.types';
import { DueDate } from '@/components/project/DueDate';
import { PriorityTag, TaskLabelTags, TaskStatusTag, TaskTypeTag } from '@/components/project/Tags';
import { TASK_PRIORITIES, TASK_STATUS_LABELS, TASK_TYPES } from '@/components/project/labels';
import { hoursText } from '@/components/project/project-utils';
import { useTaskActions } from '@/components/project/useTaskActions';
import { UserAvatar } from '@/components/user/UserAvatar';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { toOptions } from '@/shared/lib/format';

type TaskRow = Task & { children?: TaskRow[] };

interface TaskTableViewProps {
  /** 篩選後的任務（含子任務） */
  tasks: readonly Task[];
  /** 同專案全部任務，檢查前置任務用 */
  allTasks: readonly Task[];
  directory: UserDirectory;
  memberOptions: { value: string; label: string }[];
  milestones: readonly Milestone[];
  editable: boolean;
  onOpen: (taskId: number) => void;
}

/** 主任務在上層，子任務展開在底下；父任務沒被篩到時，子任務直接列在上層 */
function toRows(tasks: readonly Task[]): TaskRow[] {
  const ids = new Set(tasks.map((task) => task.id));
  const children = new Map<number, TaskRow[]>();
  for (const task of tasks) {
    if (task.parentId !== null && ids.has(task.parentId)) {
      const list = children.get(task.parentId) ?? [];
      list.push({ ...task });
      children.set(task.parentId, list);
    }
  }
  return tasks
    .filter((task) => task.parentId === null || !ids.has(task.parentId))
    .map((task) => {
      const subtasks = children.get(task.id);
      return subtasks === undefined ? { ...task } : { ...task, children: subtasks };
    });
}

/** 表格檢視：可排序、展開子任務，狀態與指派人可直接在格子裡改 */
export function TaskTableView({ tasks, allTasks, directory, memberOptions, milestones, editable, onOpen }: TaskTableViewProps) {
  const { update, confirmStatus } = useTaskActions();
  const milestoneNames = new Map(milestones.map((milestone) => [milestone.id, milestone.name] as const));

  const changeStatus = async (task: Task, status: TaskStatus): Promise<void> => {
    if (await confirmStatus(task, status, allTasks)) {
      update.mutate({ id: task.id, values: { status } });
    }
  };

  const columns: TableColumnsType<TaskRow> = [
    {
      title: '編號',
      dataIndex: 'code',
      width: 120,
      sorter: (a, b) => a.code.localeCompare(b.code),
    },
    {
      title: '類型',
      key: 'type',
      width: 110,
      sorter: (a, b) => TASK_TYPES.indexOf(a.type) - TASK_TYPES.indexOf(b.type),
      render: (_value, task) => <TaskTypeTag type={task.type} />,
    },
    {
      title: '標題',
      key: 'title',
      render: (_value, task) => (
        <Flex align="center" gap={6} wrap>
          <Typography.Link
            onClick={() => {
              onOpen(task.id);
            }}
          >
            {task.title}
          </Typography.Link>
          {task.subtaskCount !== undefined && task.subtaskCount > 0 ? (
            <Tooltip title="子任務完成數">
              <Tag icon={<BranchesOutlined />}>
                {String(task.doneSubtaskCount ?? 0)}/{String(task.subtaskCount)}
              </Tag>
            </Tooltip>
          ) : null}
          {task.predecessorIds.length > 0 ? (
            <Tooltip title={`前置任務 ${String(task.predecessorIds.length)} 個`}>
              <LinkOutlined style={{ color: '#8c8c8c' }} />
            </Tooltip>
          ) : null}
          <TaskLabelTags projectId={task.projectId} labelIds={task.labelIds} />
        </Flex>
      ),
    },
    {
      title: '狀態',
      key: 'status',
      width: 130,
      sorter: (a, b) => a.status.localeCompare(b.status),
      render: (_value, task) =>
        editable ? (
          <Select<TaskStatus>
            size="small"
            variant="borderless"
            value={task.status}
            options={toOptions(TASK_STATUS_LABELS)}
            style={{ width: 110 }}
            onChange={(status) => {
              void changeStatus(task, status);
            }}
          />
        ) : (
          <TaskStatusTag status={task.status} />
        ),
    },
    {
      title: '優先度',
      key: 'priority',
      width: 100,
      sorter: (a, b) => TASK_PRIORITIES.indexOf(a.priority) - TASK_PRIORITIES.indexOf(b.priority),
      render: (_value, task) => <PriorityTag priority={task.priority} />,
    },
    {
      title: '指派人',
      key: 'assignee',
      width: 170,
      render: (_value, task) =>
        editable ? (
          <Select<string>
            size="small"
            variant="borderless"
            allowClear
            placeholder="未指派"
            value={task.assigneeId ?? undefined}
            options={memberOptions}
            style={{ width: 150 }}
            onChange={(assigneeId: string | undefined) => {
              update.mutate({
                id: task.id,
                values: assigneeId === undefined ? { clearFields: ['assigneeId'] } : { assigneeId },
              });
            }}
          />
        ) : task.assigneeId === null ? (
          <Typography.Text type="secondary">未指派</Typography.Text>
        ) : (
          <Flex align="center" gap={6}>
            <UserAvatar userId={task.assigneeId} user={directory.user(task.assigneeId)} />
            {directory.name(task.assigneeId)}
          </Flex>
        ),
    },
    {
      title: '里程碑',
      key: 'milestone',
      width: 140,
      render: (_value, task) =>
        task.milestoneId === null ? null : (milestoneNames.get(task.milestoneId) ?? String(task.milestoneId)),
    },
    {
      title: '開始',
      dataIndex: 'startDate',
      width: 110,
      sorter: (a, b) => (a.startDate ?? '9999').localeCompare(b.startDate ?? '9999'),
    },
    {
      title: '到期',
      key: 'dueDate',
      width: 100,
      defaultSortOrder: 'ascend',
      sorter: (a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'),
      render: (_value, task) => <DueDate task={task} />,
    },
    {
      title: '工時',
      key: 'hours',
      width: 110,
      render: (_value, task) => (
        <Typography.Text type="secondary">
          {hoursText(task.loggedHours ?? 0)}
          {task.estimateHours === null ? '' : ` / ${hoursText(task.estimateHours)}`}
        </Typography.Text>
      ),
    },
  ];

  return (
    <Table<TaskRow>
      rowKey="id"
      size="middle"
      columns={columns}
      dataSource={toRows(tasks)}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${String(total)} 筆` }}
      scroll={{ x: 1110 }}
    />
  );
}
