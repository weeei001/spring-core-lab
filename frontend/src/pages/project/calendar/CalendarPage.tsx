import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Empty, Flex, Row, Select, Skeleton, Tag, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router';
import { leaveApi } from '@/api/leave/leave-api';
import { projectApi } from '@/api/project/project-api';
import { taskApi } from '@/api/project/task-api';
import { DATE_FORMAT } from '@/components/project/project-utils';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import type { PageResponse } from '@/core/http/types';
import { CalendarMonth } from './CalendarMonth';
import { dayLabel, leaveEvents, monthWeeks, taskEvent } from './calendar-events';

const COLORS = { 休假: 'green', 任務: 'blue' };

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
  // 包含月曆前後跨月的日期格
  const weeks = monthWeeks(date);
  const fromDate = weeks[0]?.format(DATE_FORMAT) ?? '';
  const toDate = weeks.at(-1)?.add(6, 'day').format(DATE_FORMAT) ?? '';
  const tasks = useQuery({
    queryKey: ['task', 'calendar', projectId],
    enabled: projectId !== undefined,
    queryFn: () => allPages((page) => taskApi.queryPage({
      // 依專案載入，避免漏掉跨越整個顯示月份或只有開始日的任務。
      projectId,
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
  const events = [
    ...leaveEvents(leaves.data ?? [], directory.name),
    ...(tasks.data ?? []).flatMap((task) => taskEvent(task) ?? []),
  ];
  const selectedDay = date.format(DATE_FORMAT);
  const selected = events.filter((event) => event.start <= selectedDay && event.end >= selectedDay);
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
      </Flex>
      {error !== null ? <Alert type="error" showIcon title="行事曆資料載入失敗" description={error.message} />
        : loading ? <Skeleton active paragraph={{ rows: 10 }} />
          : projectId === undefined ? <Empty description="尚無可查看的專案" /> : (
            <Row gutter={[16, 16]}>
              <Col xs={24} xl={17}>
                <Card styles={{ body: { overflowX: 'auto' } }}>
                  <CalendarMonth value={date} events={events} onSelect={setDate}
                    onOpen={(event) => { if (event.link !== undefined) void navigate(event.link); }} />
                </Card>
              </Col>
              <Col xs={24} xl={7}>
                <Card title={date.format('M 月 D 日 dddd')} style={{ position: 'sticky', top: 16 }}>
                  {selected.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="當天沒有事項" /> : (
                    <Flex vertical gap={12}>
                      {selected.map((entry) => (
                        <Flex key={entry.key} gap={8} align="start">
                          <Tag color={entry.overdue ? 'red' : COLORS[entry.kind]}>{dayLabel(entry, selectedDay)}</Tag>
                          <Flex vertical gap={2}>
                            {entry.link === undefined ? <Typography.Text>{entry.title}</Typography.Text>
                              : <Typography.Link onClick={() => { if (entry.link !== undefined) void navigate(entry.link); }}>{entry.title}</Typography.Link>}
                            <Typography.Text type="secondary">{entry.period}</Typography.Text>
                          </Flex>
                        </Flex>
                      ))}
                    </Flex>
                  )}
                </Card>
              </Col>
            </Row>
          )}
    </Flex>
  );
}
