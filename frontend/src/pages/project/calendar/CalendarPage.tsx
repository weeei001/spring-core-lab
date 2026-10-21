import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Calendar, Card, Empty, Flex, Select, Skeleton, Tag, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useNavigate } from 'react-router';
import { leaveApi } from '@/api/leave/leave-api';
import { projectApi } from '@/api/project/project-api';
import { taskApi } from '@/api/project/task-api';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import type { PageResponse } from '@/core/http/types';

interface CalendarEntry {
  key: string;
  title: string;
  kind: '休假' | '任務';
  link?: string;
}

const COLORS = { 休假: 'green', 任務: 'blue' };
const DATE_FORMAT = 'YYYY-MM-DD';

/** 分頁載入，避免行事曆只顯示前 100 筆卻看不出資料遭截斷。 */
async function allPages<T>(fetchPage: (page: number) => Promise<PageResponse<T>>): Promise<T[]> {
  const items: T[] = [];
  for (let page = 1; ; page++) {
    const result = await fetchPage(page);
    items.push(...result.items);
    if (items.length >= result.total || result.items.length === 0) return items;
  }
}

/** 以專案為團隊範圍，僅顯示成員休假日期，不顯示個人申請內容。 */
export function CalendarPage() {
  const navigate = useNavigate();
  const [date, setDate] = useState(() => dayjs());
  const [selectedProject, setSelectedProject] = useState<number>();
  const projects = useQuery({
    queryKey: ['project', 'calendar'],
    queryFn: () => allPages((page) => projectApi.queryPage({ page, size: 100 })),
  });
  const projectId = selectedProject ?? projects.data?.[0]?.id;
  // 包含月曆前後跨月的日期格。
  const fromDate = date.startOf('month').subtract(7, 'day').format(DATE_FORMAT);
  const toDate = date.endOf('month').add(14, 'day').format(DATE_FORMAT);
  const tasks = useQuery({
    queryKey: ['task', 'calendar', projectId, fromDate, toDate],
    enabled: projectId !== undefined,
    queryFn: () => allPages((page) => taskApi.queryPage({
      projectId, fromDueDate: fromDate, toDueDate: toDate,
      statuses: ['TODO', 'IN_PROGRESS', 'REVIEW'], page, size: 100,
    })),
  });
  const leaves = useQuery({
    queryKey: ['leave-calendar', projectId, fromDate, toDate],
    enabled: projectId !== undefined,
    queryFn: () => {
      if (projectId === undefined) throw new Error('請選擇專案');
      return leaveApi.calendar(projectId, fromDate, toDate);
    },
    staleTime: 0,
  });
  const directory = useUserDirectory((leaves.data ?? []).map((leave) => leave.applicantId));
  const entriesFor = (day: Dayjs): CalendarEntry[] => {
    const value = day.format(DATE_FORMAT);
    const absentUsers = [...new Set((leaves.data ?? [])
      .filter((leave) => leave.startDate <= value && leave.endDate >= value)
      .map((leave) => leave.applicantId))];
    return [
      ...absentUsers.map((userId): CalendarEntry => ({
        key: `leave-${userId}`, kind: '休假', title: `${directory.name(userId)} 休假`,
      })),
      ...(tasks.data ?? []).filter((task) => task.dueDate === value).map((task): CalendarEntry => ({
        key: `task-${String(task.id)}`, kind: '任務', title: `${task.code} ${task.title}`,
        link: `/projects/${String(task.projectId)}?tab=tasks&task=${String(task.id)}`,
      })),
    ];
  };
  const selected = entriesFor(date);
  const error = projects.error ?? tasks.error ?? leaves.error;
  const loading = projects.isPending || (projectId !== undefined && (tasks.isPending || leaves.isPending));

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4}>團隊行事曆</Typography.Title>
      <Flex gap={12} wrap align="center">
        <Select<number>
          aria-label="專案"
          placeholder="選擇專案"
          value={projectId}
          options={(projects.data ?? []).map((project) => ({ value: project.id, label: project.name }))}
          onChange={setSelectedProject}
          showSearch={{ optionFilterProp: 'label' }}
          style={{ width: 300, maxWidth: '100%' }}
        />
        <Button icon={<ReloadOutlined />} loading={projects.isFetching || tasks.isFetching || leaves.isFetching}
          onClick={() => {
            void projects.refetch();
            if (projectId !== undefined) {
              void tasks.refetch();
              void leaves.refetch();
            }
          }}>重新整理</Button>
        <Typography.Text type="secondary">顯示核准的休假與未完成任務期限。</Typography.Text>
      </Flex>
      {error !== null ? <Alert type="error" showIcon title="行事曆資料載入失敗" description={error.message} />
        : loading ? <Skeleton active paragraph={{ rows: 10 }} />
          : projectId === undefined ? <Empty description="尚無可查看的專案" /> : (
            <>
              <Card styles={{ body: { overflowX: 'auto' } }}>
                <Calendar value={date} onPanelChange={setDate} onSelect={setDate}
                  style={{ minWidth: 680 }}
                  cellRender={(current, info) => {
                    if (info.type !== 'date') return info.originNode;
                    const entries = entriesFor(current);
                    return <div>
                      {entries.slice(0, 2).map((entry) => (
                        <Typography.Paragraph key={entry.key} ellipsis style={{ margin: 0, fontSize: 12 }} title={entry.title}>
                          <Tag color={COLORS[entry.kind]}>{entry.kind}</Tag>{entry.title}
                        </Typography.Paragraph>
                      ))}
                      {entries.length > 2 && <Typography.Text type="secondary">另有 {entries.length - 2} 筆</Typography.Text>}
                    </div>;
                  }} />
              </Card>
              <Card title={date.format('YYYY/MM/DD')}>
                {selected.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="當天沒有事項" /> : (
                  <Flex vertical gap={12}>
                    {selected.map((entry) => (
                      <Flex key={entry.key} gap={8} align="start">
                        <Tag color={COLORS[entry.kind]}>{entry.kind}</Tag>
                        {entry.link === undefined ? <Typography.Text>{entry.title}</Typography.Text>
                          : <Typography.Link onClick={() => { if (entry.link !== undefined) void navigate(entry.link); }}>{entry.title}</Typography.Link>}
                      </Flex>
                    ))}
                  </Flex>
                )}
              </Card>
            </>
          )}
    </Flex>
  );
}
