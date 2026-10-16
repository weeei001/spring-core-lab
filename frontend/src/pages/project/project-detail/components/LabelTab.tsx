import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { App, Button, Empty, Flex, Form, Input, Popconfirm, Skeleton, Table, Typography, type TableColumnsType } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { labelApi } from '@/api/project/label-api';
import type { ProjectLabel } from '@/api/project/label.types';
import { ColorSwatches } from '@/components/project/ColorSwatches';
import { LabelTag } from '@/components/project/Tags';
import { useInvalidateProjectData, useProjectLabels, type ProjectAccess } from '@/components/project/hooks';
import { LABEL_COLORS } from '@/components/project/labels';
import { FormModal } from '@/shared/ui/FormModal';

interface LabelFormValues {
  name: string;
  color: string;
}

type Editing = { mode: 'create' } | { mode: 'edit'; label: ProjectLabel } | null;

interface LabelTabProps {
  projectId: number;
  access: ProjectAccess;
}

/** 專案標籤：例如 frontend、backend、API；經理可新增、修改、刪除，刪除時任務上的標籤一併移除 */
export function LabelTab({ projectId, access }: LabelTabProps) {
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const labels = useProjectLabels(projectId);
  const [editing, setEditing] = useState<Editing>(null);

  const save = useMutation({
    mutationFn: ({ id, values }: { id?: number; values: Partial<ProjectLabel> }) =>
      id === undefined ? labelApi.create({ ...values, projectId }) : labelApi.update(id, values),
    onSuccess: () => {
      void message.success('已儲存');
      setEditing(null);
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => labelApi.remove(id),
    onSuccess: () => {
      void message.success('已刪除標籤');
    },
    onSettled: invalidate,
  });

  if (labels.isPending) {
    return <Skeleton active paragraph={{ rows: 4 }} />;
  }
  const items = labels.data ?? [];

  const columns: TableColumnsType<ProjectLabel> = [
    { title: '標籤', key: 'name', render: (_value, label) => <LabelTag label={label} /> },
    {
      title: '任務數',
      key: 'taskCount',
      width: 120,
      render: (_value, label) => <Typography.Text type="secondary">{String(label.taskCount ?? 0)}</Typography.Text>,
    },
    ...(access.canManage
      ? [
          {
            title: '',
            key: 'actions',
            width: 100,
            render: (_value: unknown, label: ProjectLabel) => (
              <Flex gap={4}>
                <Button
                  type="text"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={() => {
                    setEditing({ mode: 'edit', label });
                  }}
                />
                <Popconfirm
                  title={`刪除標籤「${label.name}」？`}
                  description="已掛這個標籤的任務會一併移除它。"
                  okText="刪除"
                  cancelText="取消"
                  onConfirm={() => {
                    remove.mutate(label.id);
                  }}
                >
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Flex>
            ),
          },
        ]
      : []),
  ];

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={8}>
        <Typography.Text type="secondary">用標籤區分任務的範圍，例如 frontend、backend、API、UI，在任務分頁可以依標籤篩選。</Typography.Text>
        {access.canManage ? (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing({ mode: 'create' });
            }}
          >
            新增標籤
          </Button>
        ) : null}
      </Flex>

      {items.length === 0 ? (
        <Empty description="還沒有標籤" />
      ) : (
        <Table<ProjectLabel> rowKey="id" size="middle" columns={columns} dataSource={items} pagination={false} />
      )}

      <FormModal<LabelFormValues>
        open={editing !== null}
        title={editing?.mode === 'edit' ? '修改標籤' : '新增標籤'}
        initialValues={
          editing?.mode === 'edit'
            ? { name: editing.label.name, color: editing.label.color }
            : { name: '', color: LABEL_COLORS[items.length % LABEL_COLORS.length] }
        }
        saving={save.isPending}
        onCancel={() => {
          setEditing(null);
        }}
        onSubmit={(values) => {
          save.mutate({
            id: editing?.mode === 'edit' ? editing.label.id : undefined,
            values: { name: values.name.trim(), color: values.color },
          });
        }}
      >
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 30 }]}>
          <Input autoFocus placeholder="例如 frontend" />
        </Form.Item>
        <Form.Item name="color" label="顏色" rules={[{ required: true, message: '請選擇顏色' }]}>
          <ColorSwatches colors={LABEL_COLORS} />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
