import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, DatePicker, Empty, Flex, Progress, Skeleton, Table, Typography, theme, type TableColumnsType } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { UserAvatar, type UserDirectory } from '@/modules/basic-data';
import type { Task } from '../api/task.types';
import { workLogApi } from '../api/worklog-api';
import { BarChart } from '../components/BarChart';
import { useProjectTasks } from '../hooks';
import { DATE_FORMAT, hoursText, percent, weekStart } from '../project-utils';

interface WorkLogTabProps {
  projectId: number;
  directory: UserDirectory;
  onOpen: (taskId: number) => void;
}

interface MemberRow {
  userId: string;
  /** 週一日期 → 時數 */
  weeks: Map<string, number>;
  total: number;
}

const WEEKS = 8;
/** 後端統計最多 366 天 */
const MAX_WEEKS = 52;

/** 專案工時：每週合計、成員 × 週的熱度表，以及工時最多的任務的預估 vs 實際 */
export function WorkLogTab({ projectId, directory, onOpen }: WorkLogTabProps) {
  const { token } = theme.useToken();
  // 起訖都存週一
  const [range, setRange] = useState<[Dayjs, Dayjs]>(() => {
    const thisWeek = weekStart(dayjs());
    return [thisWeek.subtract(WEEKS - 1, 'week'), thisWeek];
  });
  const from = range[0];
  const to = range[1].add(6, 'day');
  const summary = useQuery({
    queryKey: ['work-log', 'summary', projectId, from.format(DATE_FORMAT), to.format(DATE_FORMAT)],
    queryFn: () => workLogApi.summary({ projectId, fromDate: from.format(DATE_FORMAT), toDate: to.format(DATE_FORMAT) }),
  });
  const tasks = useProjectTasks(projectId);

  // 區間內每一週的週一
  const weeks: string[] = [];
  for (let week = from; !week.isAfter(to); week = week.add(1, 'week')) {
    weeks.push(week.format(DATE_FORMAT));
  }
  const rows = new Map<string, MemberRow>();
  const weekTotals = new Map<string, number>();
  for (const item of summary.data ?? []) {
    const week = weekStart(dayjs(item.workDate)).format(DATE_FORMAT);
    const row = rows.get(item.userId) ?? { userId: item.userId, weeks: new Map<string, number>(), total: 0 };
    row.weeks.set(week, (row.weeks.get(week) ?? 0) + item.hours);
    row.total += item.hours;
    rows.set(item.userId, row);
    weekTotals.set(week, (weekTotals.get(week) ?? 0) + item.hours);
  }
  const memberRows = [...rows.values()].sort((a, b) => b.total - a.total);
  const maxCell = Math.max(0, ...memberRows.flatMap((row) => [...row.weeks.values()]));
  const thisWeek = weekStart(dayjs()).format(DATE_FORMAT);

  const columns: TableColumnsType<MemberRow> = [
    {
      title: '成員',
      key: 'user',
      fixed: 'left',
      width: 160,
      render: (_value, row) => (
        <Flex align="center" gap={8}>
          <UserAvatar userId={row.userId} user={directory.user(row.userId)} />
          {directory.name(row.userId)}
        </Flex>
      ),
    },
    ...weeks.map((week) => ({
      title: dayjs(week).format('MM/DD'),
      key: week,
      width: 72,
      align: 'center' as const,
      render: (_value: unknown, row: MemberRow) => {
        const hours = row.weeks.get(week) ?? 0;
        // 單一色相由淺到深：越多工時底色越深，數字一律用文字色
        const alpha = maxCell === 0 ? 0 : 0.08 + (hours / maxCell) * 0.5;
        return (
          <div
            style={{
              borderRadius: 4,
              padding: '4px 0',
              background: hours === 0 ? undefined : `rgba(22, 119, 255, ${alpha.toFixed(2)})`,
              color: hours === 0 ? token.colorTextQuaternary : token.colorText,
            }}
          >
            {hours === 0 ? '—' : String(Math.round(hours * 10) / 10)}
          </div>
        );
      },
    })),
    {
      title: '合計',
      key: 'total',
      fixed: 'right',
      width: 80,
      align: 'right',
      render: (_value, row) => <Typography.Text strong>{hoursText(row.total)}</Typography.Text>,
    },
  ];

  // 工時最多的前 10 個任務
  const topTasks: Task[] = [...(tasks.data ?? [])]
    .filter((task) => (task.loggedHours ?? 0) > 0)
    .sort((a, b) => (b.loggedHours ?? 0) - (a.loggedHours ?? 0))
    .slice(0, 10);

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Typography.Text type="secondary">依週統計，週一為一週的開始</Typography.Text>
        <DatePicker.RangePicker
          picker="week"
          allowClear={false}
          value={range}
          onChange={(value) => {
            const [start, end] = value ?? [];
            if (start != null && end != null) {
              const last = weekStart(end);
              const first = weekStart(start);
              // 超過 52 週時從結束週往前取 52 週
              setRange([last.diff(first, 'week') >= MAX_WEEKS ? last.subtract(MAX_WEEKS - 1, 'week') : first, last]);
            }
          }}
        />
      </Flex>

      {summary.isPending ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : memberRows.length === 0 ? (
        <Empty description="這段期間沒有工時紀錄" />
      ) : (
        <>
          <Card size="small" title="每週工時合計">
            <BarChart
              data={weeks.map((week) => ({
                key: week,
                label: dayjs(week).format('MM/DD'),
                value: weekTotals.get(week) ?? 0,
                tooltip: `${dayjs(week).format('MM/DD')} 那週：${hoursText(weekTotals.get(week) ?? 0)}`,
                highlight: week === thisWeek,
              }))}
              format={hoursText}
            />
          </Card>
          <Card size="small" title="成員 × 週">
            <Table<MemberRow>
              rowKey="userId"
              size="small"
              columns={columns}
              dataSource={memberRows}
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
          </Card>
        </>
      )}

      <Card size="small" title="工時最多的任務：預估 vs 已登記">
        {topTasks.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="還沒有任務登記工時" />
        ) : (
          <Flex vertical gap={10}>
            {topTasks.map((task) => {
              const logged = task.loggedHours ?? 0;
              const estimate = task.estimateHours;
              return (
                <Flex key={task.id} align="center" gap={12}>
                  <Typography.Link
                    ellipsis
                    style={{ width: 240 }}
                    onClick={() => {
                      onOpen(task.id);
                    }}
                  >
                    {task.code} {task.title}
                  </Typography.Link>
                  {estimate === null || estimate === 0 ? (
                    <Typography.Text type="secondary" style={{ flex: 1 }}>
                      未估
                    </Typography.Text>
                  ) : (
                    <Progress
                      style={{ flex: 1, margin: 0 }}
                      percent={Math.min(100, percent(logged, estimate))}
                      status={logged > estimate ? 'exception' : 'normal'}
                      showInfo={false}
                    />
                  )}
                  <Typography.Text style={{ width: 120, textAlign: 'right' }} type={estimate !== null && logged > estimate ? 'danger' : undefined}>
                    {hoursText(logged)}
                    {estimate === null ? '' : ` / ${hoursText(estimate)}`}
                  </Typography.Text>
                </Flex>
              );
            })}
          </Flex>
        )}
      </Card>
    </Flex>
  );
}
