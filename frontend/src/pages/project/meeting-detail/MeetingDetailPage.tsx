import { useCallback, useEffect, useState } from 'react';
import { skipToken, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Breadcrumb, Button, Card, DatePicker, Descriptions, Flex, Form, Input, Modal, Result, Skeleton, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { Link, useBeforeUnload, useBlocker, useNavigate, useParams } from 'react-router';
import { meetingApi } from '@/api/project/meeting-api';
import type { Meeting, MeetingInput } from '@/api/project/meeting.types';
import { useProjectContext } from '@/components/project/hooks';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { ApiError } from '@/core/http/client';
import { MeetingTasks } from './MeetingTasks';

interface MeetingForm {
  title: string;
  heldAt: Dayjs | null;
  participants: string;
  minutes: string;
}

export function MeetingDetailPage() {
  const params = useParams();
  const projectId = Number(params['id']);
  const meetingId = params['meetingId'] === 'new' ? undefined : Number(params['meetingId']);
  if (!Number.isSafeInteger(projectId) || projectId <= 0 || (meetingId !== undefined && (!Number.isSafeInteger(meetingId) || meetingId <= 0))) {
    return <Result status="404" title="找不到會議紀錄" />;
  }
  return <MeetingDetail key={`${String(projectId)}:${String(meetingId)}`} projectId={projectId} meetingId={meetingId} />;
}

function MeetingDetail({ projectId, meetingId }: { projectId: number; meetingId: number | undefined }) {
  const context = useProjectContext(projectId);
  const meeting = useQuery({
    queryKey: ['meeting', 'detail', meetingId],
    queryFn: meetingId === undefined ? skipToken : () => meetingApi.get(meetingId),
  });
  const error = meeting.error ?? context.error;
  if (error && (!context.project || (meetingId !== undefined && !meeting.data))) {
    const status = error instanceof ApiError ? error.status : undefined;
    return <Result status={status === 403 ? '403' : status === 404 ? '404' : 'error'} title={error.message} extra={<Button onClick={() => { context.retry(); if (meetingId !== undefined) { void meeting.refetch(); } }}>重試</Button>} />;
  }
  if (context.loading || (meetingId !== undefined && meeting.isPending)) {
    return <Skeleton active paragraph={{ rows: 10 }} />;
  }
  if (!context.project || (meeting.data && meeting.data.projectId !== projectId)) {
    return <Result status="404" title="找不到會議紀錄或所屬專案" />;
  }
  return <MeetingEditor projectId={projectId} initial={meeting.data} />;
}

function MeetingEditor({ projectId, initial }: { projectId: number; initial: Meeting | undefined }) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user, can } = useAuth();
  const { project, members, access } = useProjectContext(projectId);
  const [record, setRecord] = useState(initial);
  const [dirty, setDirty] = useState(false);
  const [form] = Form.useForm<MeetingForm>();
  const directory = useUserDirectory([record?.transId]);
  const back = `/projects/${String(projectId)}?tab=meetings`;
  const canEdit = access.isMember && !access.closed && can(PERMISSIONS.MEETING_WRITE)
    && (record === undefined || record.createId === String(user.id) || access.myRole === 'MANAGER' || can(PERMISSIONS.PROJECT_ADMIN));
  const blocker = useBlocker(dirty);
  useBeforeUnload(useCallback((event) => { if (dirty) { event.preventDefault(); } }, [dirty]));

  const save = useMutation({
    mutationFn: (body: MeetingInput) => record === undefined ? meetingApi.create({ ...body, projectId }) : meetingApi.update(record.id, body),
    onSuccess: (saved) => {
      setRecord(saved);
      setDirty(false);
      form.setFieldsValue({ title: saved.title, heldAt: dayjs(saved.heldAt), participants: saved.participants, minutes: saved.minutes });
      queryClient.setQueryData(['meeting', 'detail', saved.id], saved);
      void queryClient.invalidateQueries({ queryKey: ['meeting', 'project'] });
      void message.success('已儲存會議紀錄');
    },
  });
  useEffect(() => {
    if (initial === undefined && record !== undefined) {
      void navigate(`/projects/${String(projectId)}/meetings/${String(record.id)}`, { replace: true });
    }
  }, [initial, record, projectId, navigate]);

  return (
    <Flex vertical gap={16} style={{ minWidth: 0 }}>
      <Breadcrumb items={[{ title: <Link to="/projects">專案</Link> }, { title: <Link to={back}>{project?.name ?? '所屬專案'}</Link> }, { title: record?.title ?? '新增會議紀錄' }]} />
      <Card>
        {access.closed ? <Alert type="info" title="專案已結案或取消，會議紀錄為唯讀" style={{ marginBottom: 16 }} /> : null}
        {save.error ? <Alert type="error" title={save.error.message} style={{ marginBottom: 16 }} /> : null}
        {record && !canEdit && !dirty ? (
          <Flex vertical gap={20}>
            <Typography.Title level={3} style={{ overflowWrap: 'anywhere' }}>{record.title}</Typography.Title>
            <Descriptions column={1} items={[
              { key: 'time', label: '會議時間', children: dayjs(record.heldAt).format('YYYY-MM-DD HH:mm') },
              { key: 'participants', label: '參與者', children: <span style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{record.participants}</span> },
            ]} />
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{record.minutes}</Typography.Paragraph>
          </Flex>
        ) : (
          <Form<MeetingForm>
            form={form}
            layout="vertical"
            disabled={!canEdit || save.isPending}
            initialValues={{ title: record?.title ?? '', heldAt: record ? dayjs(record.heldAt) : dayjs(), participants: record?.participants ?? '', minutes: record?.minutes ?? '' }}
            onValuesChange={(_, values: MeetingForm) => {
              setDirty(record === undefined || values.title !== record.title || values.participants !== record.participants || values.minutes !== record.minutes || values.heldAt === null || !values.heldAt.isSame(dayjs(record.heldAt)));
            }}
            onFinish={(values) => {
              if (values.heldAt !== null) {
                save.mutate({ title: values.title, heldAt: values.heldAt.format('YYYY-MM-DDTHH:mm:ss'), participants: values.participants, minutes: values.minutes });
              }
            }}
          >
            <Form.Item name="title" label="標題" rules={[{ required: true, whitespace: true, message: '請填寫標題' }, { max: 200 }]}><Input maxLength={200} showCount /></Form.Item>
            <Form.Item name="heldAt" label="會議時間" rules={[{ required: true, message: '請填寫會議時間' }]}><DatePicker showTime format="YYYY-MM-DD HH:mm:ss" style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="participants" label="參與者" rules={[{ required: true, whitespace: true, message: '請填寫參與者' }, { max: 2000 }]}><Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} maxLength={2000} showCount /></Form.Item>
            <Form.Item name="minutes" label="紀錄／決議" rules={[{ required: true, whitespace: true, message: '請填寫紀錄／決議' }, { max: 10000 }]}><Input.TextArea autoSize={{ minRows: 12, maxRows: 30 }} maxLength={10000} showCount /></Form.Item>
            <Flex gap={8} wrap>
              {canEdit ? <Button type="primary" htmlType="submit" loading={save.isPending}>儲存</Button> : null}
              <Button disabled={save.isPending} onClick={() => { void navigate(back); }}>取消</Button>
            </Flex>
          </Form>
        )}
        {record ? <Typography.Paragraph type="secondary" style={{ marginTop: 20 }}>最後修改：{directory.name(record.transId)} · {dayjs(record.transTime).format('YYYY-MM-DD HH:mm:ss')}</Typography.Paragraph> : null}
        {record && !canEdit && !dirty ? <Link to={back}>返回會議清單</Link> : null}
      </Card>
      {record ? <MeetingTasks meetingId={record.id} projectId={projectId} members={members} canEdit={canEdit} canCreate={access.canEditTasks} /> : null}
      <Modal open={blocker.state === 'blocked'} title="放棄未儲存的會議紀錄？" okText="放棄並離開" cancelText="繼續編輯" onOk={() => { if (blocker.state === 'blocked') { blocker.proceed(); } }} onCancel={() => { if (blocker.state === 'blocked') { blocker.reset(); } }}>
        離開後會失去尚未儲存的內容。
      </Modal>
    </Flex>
  );
}
