import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, DatePicker, Empty, Flex, Form, Input, InputNumber, Popconfirm, Progress, Typography } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useAuth } from '@/core/auth/useAuth';
import { UserAvatar, useUserDirectory } from '@/modules/basic-data';
import type { Task } from '../../api/task.types';
import { workLogApi } from '../../api/worklog-api';
import { useInvalidateProjectData } from '../../hooks';
import { DATE_FORMAT, hoursText, percent } from '../../project-utils';

interface WorkLogFormValues {
  workDate: Dayjs;
  hours: number;
  note?: string;
}

interface TaskWorkLogsProps {
  task: Task;
  editable: boolean;
}

/** 任務的工時：預估 vs 已登記、明細，以及登記自己的工時 */
export function TaskWorkLogs({ task, editable }: TaskWorkLogsProps) {
  const { user } = useAuth();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const [form] = Form.useForm<WorkLogFormValues>();
  const logs = useQuery({
    queryKey: ['work-log', 'task', task.id],
    queryFn: () => workLogApi.query({ taskId: task.id }),
    select: (items) => [...items].sort((a, b) => b.workDate.localeCompare(a.workDate) || b.id - a.id),
  });
  const directory = useUserDirectory((logs.data ?? []).map((log) => log.userId));

  const create = useMutation({
    mutationFn: (values: WorkLogFormValues) =>
      workLogApi.create({ taskId: task.id, workDate: values.workDate.format(DATE_FORMAT), hours: values.hours, note: values.note }),
    onSuccess: () => {
      void message.success('已登記工時');
      form.resetFields(['hours', 'note']);
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => workLogApi.remove(id),
    onSettled: invalidate,
  });

  const total = (logs.data ?? []).reduce((sum, log) => sum + log.hours, 0);
  const estimate = task.estimateHours;

  return (
    <Flex vertical gap={16}>
      <Flex vertical gap={4}>
        <Typography.Text>
          已登記 <Typography.Text strong>{hoursText(total)}</Typography.Text>
          {estimate === null ? '' : ` / 預估 ${hoursText(estimate)}`}
        </Typography.Text>
        {estimate === null || estimate === 0 ? null : (
          <Progress
            percent={Math.min(100, percent(total, estimate))}
            status={total > estimate ? 'exception' : 'active'}
            format={() => `${String(percent(total, estimate))}%`}
          />
        )}
      </Flex>

      {editable ? (
        <Form<WorkLogFormValues>
          form={form}
          layout="inline"
          initialValues={{ workDate: dayjs() }}
          onFinish={(values) => {
            create.mutate(values);
          }}
          style={{ rowGap: 8 }}
        >
          <Form.Item name="workDate" rules={[{ required: true, message: '請選日期' }]}>
            <DatePicker allowClear={false} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="hours" rules={[{ required: true, message: '請輸入時數' }]}>
            <InputNumber min={0.5} max={24} step={0.5} precision={1} placeholder="時數" suffix="h" style={{ width: 100 }} />
          </Form.Item>
          <Form.Item name="note" rules={[{ max: 500 }]} style={{ flex: 1, minWidth: 140 }}>
            <Input placeholder="備註（選填）" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<PlusOutlined />} loading={create.isPending}>
              登記
            </Button>
          </Form.Item>
        </Form>
      ) : null}

      {(logs.data ?? []).length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="還沒有工時紀錄" />
      ) : (
        <Flex vertical gap={8}>
          {(logs.data ?? []).map((log) => (
            <Flex key={log.id} align="center" gap={12}>
              <UserAvatar userId={log.userId} user={directory.user(log.userId)} />
              <Typography.Text style={{ width: 90 }}>{log.workDate}</Typography.Text>
              <Typography.Text strong style={{ width: 50 }}>
                {hoursText(log.hours)}
              </Typography.Text>
              <Typography.Text type="secondary" ellipsis style={{ flex: 1 }}>
                {log.note ?? ''}
              </Typography.Text>
              {editable && log.userId === String(user.id) ? (
                <Popconfirm
                  title="刪除這筆工時？"
                  okText="刪除"
                  cancelText="取消"
                  onConfirm={() => {
                    remove.mutate(log.id);
                  }}
                >
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              ) : null}
            </Flex>
          ))}
        </Flex>
      )}
    </Flex>
  );
}
