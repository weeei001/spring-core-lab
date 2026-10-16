import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, DatePicker, Empty, Flex, InputNumber, Skeleton, Table, Tag, Tooltip, Typography, theme, type TableColumnsType } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useAuth } from '@/core/auth/useAuth';
import { PERMISSIONS } from '@/core/auth/permissions';
import { PageTour } from '@/shared/ui/PageTour';
import { projectApi } from '../api/project-api';
import { taskApi } from '../api/task-api';
import type { Task } from '../api/task.types';
import { workLogApi } from '../api/worklog-api';
import type { WorkLog } from '../api/worklog.types';
import { TaskDrawer } from '../components/task-drawer/TaskDrawer';
import { useInvalidateProjectData, useTaskDrawer } from '../hooks';
import { DEFAULT_PROJECT_COLOR } from '../labels';
import { DATE_FORMAT, hoursText, isProjectClosed, weekStart } from '../project-utils';
import { TIMESHEET_TOUR } from '../tours';

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];

interface Row {
  task: Task;
  /** YYYY-MM-DD → 那天的工時紀錄（通常 0 或 1 筆） */
  logs: Map<string, WorkLog[]>;
  total: number;
}

interface CellChange {
  task: Task;
  date: string;
  existing: WorkLog[];
  hours: number | null;
}

/**
 * 工時週表：列是任務（指派給我的未完成任務 + 這週有登記過的任務），欄是週一到週日，
 * 直接在格子裡填時數，離開格子就儲存；填 0 或清空即刪除。同一天有多筆時請到任務抽屜修改。
 */
export function TimesheetPage() {
  const { user, can } = useAuth();
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const drawer = useTaskDrawer();
  const myId = String(user.id);
  const [week, setWeek] = useState<Dayjs>(() => weekStart(dayjs()));
  const days = Array.from({ length: 7 }, (_, index) => week.add(index, 'day').format(DATE_FORMAT));
  const from = days[0] ?? week.format(DATE_FORMAT);
  const to = days[6] ?? week.add(6, 'day').format(DATE_FORMAT);
  const today = dayjs().format(DATE_FORMAT);

  // 明確給 userId / assigneeId：有 project:admin 的人不受後端限縮
  const logs = useQuery({
    queryKey: ['work-log', 'mine', myId, from],
    queryFn: () => workLogApi.query({ userId: myId, fromDate: from, toDate: to }),
  });
  const myTasks = useQuery({
    queryKey: ['task', 'mine', myId, false],
    queryFn: () => taskApi.queryAll({ assigneeId: myId, statuses: ['TODO', 'IN_PROGRESS', 'REVIEW'] }),
  });
  const knownIds = new Set((myTasks.data ?? []).map((task) => task.id));
  const missingIds = [...new Set((logs.data ?? []).map((log) => log.taskId))].filter((id) => !knownIds.has(id)).sort((a, b) => a - b);
  const loggedTasks = useQuery({
    queryKey: ['task', 'by-ids', missingIds],
    queryFn: () => taskApi.query({ ids: missingIds }),
    enabled: missingIds.length > 0,
  });
  const tasks = [...(myTasks.data ?? []), ...(loggedTasks.data ?? [])];
  const projectIds = [...new Set(tasks.map((task) => task.projectId))].sort((a, b) => a - b);
  const projects = useQuery({
    queryKey: ['project', 'by-ids', projectIds],
    queryFn: () => projectApi.query({ ids: projectIds }),
    enabled: projectIds.length > 0,
  });
  const projectOf = (id: number) => projects.data?.find((project) => project.id === id);

  const save = useMutation({
    mutationFn: async ({ task, date, existing, hours }: CellChange) => {
      const current = existing[0];
      if (current === undefined) {
        if (hours !== null && hours > 0) {
          await workLogApi.create({ taskId: task.id, workDate: date, hours });
        }
      } else if (hours === null || hours === 0) {
        await workLogApi.remove(current.id);
      } else if (hours !== current.hours) {
        await workLogApi.update(current.id, { hours });
      }
    },
    onSuccess: () => {
      void message.success('已儲存工時');
    },
    onSettled: invalidate,
  });

  const rows: Row[] = tasks
    .map((task) => {
      const byDay = new Map<string, WorkLog[]>();
      let total = 0;
      for (const log of logs.data ?? []) {
        if (log.taskId === task.id) {
          byDay.set(log.workDate, [...(byDay.get(log.workDate) ?? []), log]);
          total += log.hours;
        }
      }
      return { task, logs: byDay, total };
    })
    // 依專案、編號；不依工時排序，填完第一格列才不會跳位置
    .sort((a, b) => a.task.projectId - b.task.projectId || a.task.code.localeCompare(b.task.code));

  const dayTotal = (date: string): number =>
    (logs.data ?? []).filter((log) => log.workDate === date).reduce((sum, log) => sum + log.hours, 0);
  const weekTotal = (logs.data ?? []).reduce((sum, log) => sum + log.hours, 0);

  const columns: TableColumnsType<Row> = [
    {
      title: '任務',
      key: 'task',
      fixed: 'left',
      width: 280,
      render: (_value, row) => {
        const project = projectOf(row.task.projectId);
        return (
          <Flex vertical gap={2}>
            <Typography.Link
              ellipsis
              onClick={() => {
                drawer.open(row.task.id);
              }}
            >
              {row.task.title}
            </Typography.Link>
            {project === undefined ? null : (
              <span>
                <Tag color={project.color ?? DEFAULT_PROJECT_COLOR}>{project.name}</Tag>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {row.task.code}
                </Typography.Text>
              </span>
            )}
          </Flex>
        );
      },
    },
    ...days.map((date, index) => ({
      title: (
        <Flex vertical align="center">
          <span>週{WEEKDAYS[index] ?? ''}</span>
          <Typography.Text type={date === today ? undefined : 'secondary'} strong={date === today} style={{ fontSize: 12 }}>
            {dayjs(date).format('MM/DD')}
          </Typography.Text>
        </Flex>
      ),
      key: date,
      width: 96,
      align: 'center' as const,
      onCell: () => ({ style: date === today ? { background: token.colorPrimaryBg } : {} }),
      render: (_value: unknown, row: Row) => {
        const existing = row.logs.get(date) ?? [];
        const hours = existing.reduce((sum, log) => sum + log.hours, 0);
        const project = projectOf(row.task.projectId);
        const editable = can(PERMISSIONS.WORK_LOG_WRITE) && project !== undefined && !isProjectClosed(project.status);
        if (existing.length > 1 || !editable) {
          return (
            <Tooltip title={existing.length > 1 ? '這天有多筆紀錄，請到任務抽屜修改' : undefined}>
              <Typography.Text type={hours === 0 ? 'secondary' : undefined}>{hours === 0 ? '—' : String(hours)}</Typography.Text>
            </Tooltip>
          );
        }
        return (
          <InputNumber
            key={`${String(row.task.id)}-${date}-${String(hours)}`}
            size="small"
            variant="filled"
            min={0}
            max={24}
            step={0.5}
            precision={1}
            controls={false}
            placeholder="0"
            defaultValue={hours === 0 ? undefined : hours}
            style={{ width: 64 }}
            onPressEnter={(event) => {
              event.currentTarget.blur();
            }}
            onBlur={(event) => {
              const text = event.target.value.trim();
              const next = text === '' ? null : Number(text);
              // 這裡比 InputNumber 自己的範圍修正早執行，拿到的是原始輸入，要自己檢查
              if (next !== null && (Number.isNaN(next) || next < 0 || next > 24)) {
                void message.warning('工時要在 0 到 24 小時之間');
                return;
              }
              if (next !== null && next * 2 !== Math.round(next * 2)) {
                void message.warning('工時以 0.5 小時為單位');
                return;
              }
              if ((next ?? 0) !== hours) {
                save.mutate({ task: row.task, date, existing, hours: next });
              }
            }}
          />
        );
      },
    })),
    {
      title: '合計',
      key: 'total',
      fixed: 'right',
      width: 80,
      align: 'right',
      render: (_value, row) => (row.total === 0 ? <Typography.Text type="secondary">—</Typography.Text> : <Typography.Text strong>{hoursText(row.total)}</Typography.Text>),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          工時
        </Typography.Title>
        <Flex gap={8} align="center" data-tour="timesheet-week">
          <Button
            icon={<LeftOutlined />}
            onClick={() => {
              setWeek(week.subtract(1, 'week'));
            }}
          />
          <DatePicker
            picker="week"
            allowClear={false}
            value={week}
            onChange={(value: Dayjs | null) => {
              if (value !== null) {
                setWeek(weekStart(value));
              }
            }}
          />
          <Button
            icon={<RightOutlined />}
            onClick={() => {
              setWeek(week.add(1, 'week'));
            }}
          />
          <Button
            onClick={() => {
              setWeek(weekStart(dayjs()));
            }}
          >
            本週
          </Button>
        </Flex>
      </Flex>

      {logs.isPending || myTasks.isPending ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : rows.length === 0 ? (
        <Empty description="這週沒有工時，也沒有指派給你的未完成任務" />
      ) : (
        <div data-tour="timesheet-table">
          <Table<Row>
            rowKey={(row) => row.task.id}
            size="middle"
            columns={columns}
            dataSource={rows}
            pagination={false}
            scroll={{ x: 'max-content' }}
            summary={() => (
              <Table.Summary fixed>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0}>
                    <Typography.Text strong>每日合計</Typography.Text>
                  </Table.Summary.Cell>
                  {days.map((date, index) => {
                    const total = dayTotal(date);
                    return (
                      <Table.Summary.Cell key={date} index={index + 1} align="center">
                        <Typography.Text strong type={total > 8 ? 'warning' : total === 0 ? 'secondary' : undefined}>
                          {total === 0 ? '—' : hoursText(total)}
                        </Typography.Text>
                      </Table.Summary.Cell>
                    );
                  })}
                  <Table.Summary.Cell index={8} align="right">
                    <Typography.Text strong style={{ color: token.colorPrimary }}>
                      {hoursText(weekTotal)}
                    </Typography.Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              </Table.Summary>
            )}
          />
        </div>
      )}

      <TaskDrawer taskId={drawer.taskId} onClose={drawer.close} onOpen={drawer.open} />
      <PageTour id="timesheet" steps={TIMESHEET_TOUR} />
    </Flex>
  );
}
