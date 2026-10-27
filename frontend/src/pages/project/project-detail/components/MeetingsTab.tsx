import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, DatePicker, Flex, Table } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { Link } from 'react-router';
import { meetingApi } from '@/api/project/meeting-api';
import type { Meeting } from '@/api/project/meeting.types';
import type { ProjectAccess } from '@/components/project/hooks';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';

export function MeetingsTab({ projectId, access }: { projectId: number; access: ProjectAccess }) {
  const { can } = useAuth();
  const [page, setPage] = useState(1);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const query = {
    projectId,
    page,
    size: 20,
    fromHeldAt: range?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
    toHeldAt: range?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
  };
  const meetings = useQuery({ queryKey: ['meeting', 'project', query], queryFn: () => meetingApi.queryPage(query) });
  const directory = useUserDirectory((meetings.data?.items ?? []).map((meeting) => meeting.transId));

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" wrap gap={12}>
        <DatePicker.RangePicker
          showTime
          value={range}
          placeholder={['會議時間起', '會議時間迄']}
          style={{ maxWidth: '100%' }}
          onChange={(value) => { setRange(value); setPage(1); }}
        />
        {access.isMember && !access.closed && can(PERMISSIONS.MEETING_WRITE) ? (
          <Link to={`/projects/${String(projectId)}/meetings/new`}>
            <Button type="primary" icon={<PlusOutlined />}>新增會議紀錄</Button>
          </Link>
        ) : null}
      </Flex>
      {meetings.error ? <Alert type="error" title={meetings.error.message} action={<Button onClick={() => { void meetings.refetch(); }}>重試</Button>} /> : null}
      <Table<Meeting>
        rowKey="id"
        loading={meetings.isPending}
        dataSource={meetings.data?.items ?? []}
        scroll={{ x: 760 }}
        locale={{ emptyText: meetings.isError ? '會議紀錄載入失敗' : '尚無符合的會議紀錄' }}
        pagination={{ current: page, pageSize: 20, total: meetings.data?.total ?? 0, showSizeChanger: false, onChange: setPage }}
        columns={[
          { title: '標題', dataIndex: 'title', width: 260, render: (_, meeting) => <Link style={{ overflowWrap: 'anywhere' }} to={`/projects/${String(projectId)}/meetings/${String(meeting.id)}`}>{meeting.title}</Link> },
          { title: '會議時間', dataIndex: 'heldAt', width: 170, render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
          { title: '最後修改人', dataIndex: 'transId', render: (value: string) => directory.name(value) },
          { title: '最後修改時間', dataIndex: 'transTime', width: 170, render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm') },
        ]}
      />
    </Flex>
  );
}
