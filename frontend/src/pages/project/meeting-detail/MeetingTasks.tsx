import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, DatePicker, Flex, Form, Input, Modal, Popconfirm, Select, Skeleton, Tag, Typography } from 'antd';
import type { Dayjs } from 'dayjs';
import { Link } from 'react-router';
import { meetingApi } from '@/api/project/meeting-api';
import type { MeetingTaskInput } from '@/api/project/meeting.types';
import { taskApi } from '@/api/project/task-api';
import type { ProjectMember } from '@/api/project/member.types';
import { TASK_STATUS_LABELS } from '@/components/project/labels';
import { useMemberDirectory } from '@/components/project/useMemberDirectory';
import { ApiError } from '@/core/http/client';
import { useDebounced } from '@/shared/hooks/useDebounced';

interface TaskForm {
  title: string;
  assigneeId?: string;
  dueDate?: Dayjs | null;
}

export function MeetingTasks({ meetingId, projectId, members, canEdit, canCreate }: {
  meetingId: number;
  projectId: number;
  members: ProjectMember[];
  canEdit: boolean;
  canCreate: boolean;
}) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<{ value: number; label: string }>();
  const [search, setSearch] = useState('');
  const keyword = useDebounced(search.trim());
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<TaskForm>();
  const tasks = useQuery({ queryKey: ['meeting', 'tasks', meetingId], queryFn: () => meetingApi.tasks(meetingId), staleTime: 0 });
  const candidates = useQuery({ queryKey: ['task', 'project', projectId, 'meeting-search', keyword], queryFn: () => taskApi.queryAll({ projectId, ...(keyword === '' ? {} : { keyword }) }), enabled: canEdit });
  const { directory, options: memberOptions } = useMemberDirectory(members, (tasks.data ?? []).map((task) => task.assigneeId));
  const refresh = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['meeting'] }),
      queryClient.invalidateQueries({ queryKey: ['task'] }),
    ]);
  };
  const link = useMutation({ mutationFn: (taskId: number) => meetingApi.linkTask(meetingId, taskId), onSuccess: async () => { setSelected(undefined); await refresh(); } });
  const unlink = useMutation({ mutationFn: (taskId: number) => meetingApi.unlinkTask(meetingId, taskId), onSuccess: refresh });
  const create = useMutation({ mutationFn: (body: MeetingTaskInput) => meetingApi.createTask(meetingId, body), onSuccess: async () => { setOpen(false); form.resetFields(); await refresh(); } });
  const busy = link.isPending || unlink.isPending || create.isPending;
  const createUnknown = create.error instanceof ApiError && (create.error.status === undefined || create.error.status >= 500);
  const linked = new Set((tasks.data ?? []).map((task) => task.taskId));
  const options = (candidates.data ?? []).filter((task) => !linked.has(task.id)).map((task) => ({ value: task.id, label: `${task.code} ${task.title}` }));
  if (selected !== undefined && !options.some((option) => option.value === selected.value)) {
    options.unshift(selected);
  }
  const error = tasks.error ?? link.error ?? unlink.error ?? create.error;

  return (
    <Card title="會議待辦">
      <Flex vertical gap={12}>
        {error ? <Alert type="error" title={error.message} action={tasks.isError ? <Button onClick={() => { void tasks.refetch(); }}>重試</Button> : undefined} /> : null}
        {tasks.isPending ? <Skeleton active paragraph={{ rows: 2 }} /> : tasks.isError ? null : tasks.data.length === 0 ? (
          <Typography.Text type="secondary">尚無連結任務</Typography.Text>
        ) : tasks.data.map((task) => (
          <Flex key={task.taskId} align="center" gap={8} wrap>
            {task.deleted ? <Typography.Text type="secondary">任務已刪除（{task.taskId}）</Typography.Text> : <>
              <Link to={`/tasks/${String(task.taskId)}`}>{task.code} {task.title}</Link>
              {task.status === null ? null : <Tag>{TASK_STATUS_LABELS[task.status]}</Tag>}
              <Typography.Text type="secondary">{task.assigneeId === null ? '未指派' : directory.name(task.assigneeId)} · {task.dueDate ?? '無期限'}</Typography.Text>
            </>}
            {canEdit ? <Popconfirm title="解除任務連結？" description="任務不會被刪除。" okText="解除" cancelText="取消" onConfirm={() => { unlink.mutate(task.taskId); }}><Button size="small" disabled={busy}>解除連結</Button></Popconfirm> : null}
          </Flex>
        ))}
        {canEdit ? <Flex gap={8} wrap>
          <Select<number> value={selected?.value} onChange={(value) => { setSelected(options.find((option) => option.value === value)); }} disabled={busy} loading={candidates.isFetching} placeholder="搜尋同專案任務" showSearch={{ filterOption: false, onSearch: setSearch }} style={{ minWidth: 240 }} options={options} />
          <Button disabled={busy || selected === undefined} loading={link.isPending} onClick={() => { if (selected !== undefined) { link.mutate(selected.value); } }}>連結任務</Button>
          {canCreate ? <Button disabled={busy} onClick={() => { create.reset(); setOpen(true); }}>新增任務</Button> : null}
        </Flex> : null}
        {candidates.isError ? <Alert type="error" title={candidates.error.message} action={<Button onClick={() => { void candidates.refetch(); }}>重試</Button>} /> : null}
      </Flex>
      <Modal title="新增會議待辦" open={open} confirmLoading={create.isPending} okText="建立並連結" cancelText="取消" onCancel={() => { if (!busy) { setOpen(false); if (createUnknown) { void refresh(); } } }} onOk={() => { form.submit(); }} okButtonProps={{ disabled: busy || createUnknown }} cancelButtonProps={{ disabled: busy }} mask={{ closable: !busy }} keyboard={!busy}>
        <Form<TaskForm> form={form} layout="vertical" disabled={busy} onFinish={(values) => { if (!busy && !createUnknown) { create.mutate({ title: values.title, assigneeId: values.assigneeId, dueDate: values.dueDate?.format('YYYY-MM-DD') }); } }}>
          <Form.Item name="title" label="標題" rules={[{ required: true, whitespace: true, message: '請輸入標題' }, { max: 200 }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="assigneeId" label="負責人"><Select allowClear placeholder="未指派" options={memberOptions} showSearch={{ optionFilterProp: 'label' }} /></Form.Item>
          <Form.Item name="dueDate" label="期限"><DatePicker style={{ width: '100%' }} /></Form.Item>
          {create.error ? <Alert type="error" title={create.error.message} /> : null}
          {createUnknown ? <Alert type="warning" title="無法確認建立結果，請先查看會議待辦或專案任務清單，再決定是否重試。" /> : null}
        </Form>
      </Modal>
    </Card>
  );
}
