import { Flex, Select, Table, Tag, Tooltip, Typography, type TableColumnsType } from 'antd';
import { BranchesOutlined, LinkOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router';
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
export function TaskTableView({ tasks, allTasks, directory, memberOptions, editable, onOpen }: TaskTableViewProps) {
  const { update, confirmStatus } = useTaskActions();
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('taskPage'));
  const pageSize = Number(params.get('taskPageSize'));
  const sort = params.get('taskSort') ?? 'dueDate';
  const order = params.get('taskOrder') === 'descend' ? 'descend' : 'ascend';
  const expanded = (params.get('taskExpanded') ?? '').split(',').map(Number).filter((id) => Number.isSafeInteger(id) && id > 0);

  const changeStatus = async (task: Task, status: TaskStatus): Promise<void> => {
    if (await confirmStatus(task, status, allTasks)) {
      update.mutate({ id: task.id, values: { status } });
    }
  };

  const columns: TableColumnsType<TaskRow> = [
    {
      title: '編號',
      key: 'code',
      dataIndex: 'code',
      width: 120,
      sorter: (a, b) => a.code.localeCompare(b.code),
    },
    {
      title: '類型',
      key: 'type',
      width: 110,
      responsive: ['md'],
      sorter: (a, b) => TASK_TYPES.indexOf(a.type) - TASK_TYPES.indexOf(b.type),
      render: (_value, task) => <TaskTypeTag type={task.type} />,
    },
    {
      title: '標題',
      key: 'title',
      width: 280,
      render: (_value, task) => {
        // 有 href 才能用 Tab 聚焦並以 Enter 開啟；網址與抽屜的 ?task= 相同。
        const search = new URLSearchParams(params);
        search.set('task', String(task.id));
        return (
          <Flex align="center" gap={6} wrap>
            <Typography.Link
              href={`?${search.toString()}`}
              onClick={(event) => {
                event.preventDefault();
                onOpen(task.id);
              }}
            >
              <span style={{ overflowWrap: 'anywhere' }}>{task.title}</span>
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
        );
      },
    },
    {
      title: '狀態',
      key: 'status',
      width: 130,
      sorter: (a, b) => a.status.localeCompare(b.status),
      render: (_value, task) =>
        editable ? (
          <Select<TaskStatus>
            aria-label={`${task.code} 狀態`}
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
      responsive: ['md'],
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
            aria-label={`${task.code} 指派人`}
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
      title: '開始',
      key: 'startDate',
      dataIndex: 'startDate',
      width: 110,
      responsive: ['lg'],
      sorter: (a, b) => (a.startDate ?? '9999').localeCompare(b.startDate ?? '9999'),
    },
    {
      title: '到期',
      key: 'dueDate',
      width: 100,
      sorter: (a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'),
      render: (_value, task) => <DueDate task={task} />,
    },
    {
      title: '工時',
      key: 'hours',
      width: 110,
      responsive: ['lg'],
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
      columns={columns.map((column) => ({ ...column, sortOrder: column.key === sort ? order : null }))}
      dataSource={toRows(tasks)}
      pagination={{
        current: Number.isSafeInteger(page) && page > 0 ? page : 1,
        pageSize: [10, 20, 50, 100].includes(pageSize) ? pageSize : 20,
        showSizeChanger: true,
        showTotal: (total) => `共 ${String(total)} 筆`,
      }}
      expandable={{
        expandedRowKeys: expanded,
        onExpandedRowsChange: (keys) => {
          setParams((current) => {
            const next = new URLSearchParams(current);
            if (keys.length === 0) {
              next.delete('taskExpanded');
            } else {
              next.set('taskExpanded', keys.join(','));
            }
            return next;
          }, { replace: true, preventScrollReset: true });
        },
      }}
      onChange={(pagination, _filters, sorter, extra) => {
        const sorted = Array.isArray(sorter) ? sorter[0] : sorter;
        setParams((current) => {
          const next = new URLSearchParams(current);
          next.set('taskPage', String(pagination.current ?? 1));
          next.set('taskPageSize', String(pagination.pageSize ?? 20));
          next.set('taskSort', sorted?.order == null ? 'none' : String(sorted.columnKey));
          if (sorted?.order == null) {
            next.delete('taskOrder');
          } else {
            next.set('taskOrder', sorted.order);
          }
          return next;
        }, { replace: extra.action !== 'paginate', preventScrollReset: true });
      }}
      scroll={{ x: 'max-content' }}
    />
  );
}
