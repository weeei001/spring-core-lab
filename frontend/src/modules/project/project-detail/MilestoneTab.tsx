import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { App, Button, Card, DatePicker, Empty, Flex, Form, Input, Popconfirm, Progress, Skeleton, Switch, Tag, Typography, theme } from 'antd';
import { CheckOutlined, DeleteOutlined, EditOutlined, FlagOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { motion } from 'motion/react';
import { FormModal } from '@/shared/ui/FormModal';
import { milestoneApi } from '../api/milestone-api';
import type { Milestone } from '../api/milestone.types';
import { useInvalidateProjectData, useProjectMilestones, type ProjectAccess } from '../hooks';
import { DATE_FORMAT, dueText, hasText, percent, todayText } from '../project-utils';

interface MilestoneFormValues {
  name: string;
  description?: string;
  dueDate: Dayjs;
}

type Editing = { mode: 'create' } | { mode: 'edit'; milestone: Milestone } | null;

interface MilestoneTabProps {
  projectId: number;
  access: ProjectAccess;
}

/** 里程碑時間軸：依到期日由上而下，進度是底下主任務的完成比例；經理可新增、修改、標記達成 */
export function MilestoneTab({ projectId, access }: MilestoneTabProps) {
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const milestones = useProjectMilestones(projectId);
  const [editing, setEditing] = useState<Editing>(null);

  const save = useMutation({
    mutationFn: ({ id, values }: { id?: number; values: Partial<Milestone> }) =>
      id === undefined ? milestoneApi.create({ ...values, projectId }) : milestoneApi.update(id, values),
    onSuccess: () => {
      void message.success('已儲存');
      setEditing(null);
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => milestoneApi.remove(id),
    onSuccess: () => {
      void message.success('已刪除里程碑');
    },
    onSettled: invalidate,
  });

  if (milestones.isPending) {
    return <Skeleton active paragraph={{ rows: 6 }} />;
  }
  const items = milestones.data ?? [];
  const today = todayText();

  return (
    <Flex vertical gap={16}>
      {access.canManage ? (
        <Flex justify="end">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing({ mode: 'create' });
            }}
          >
            新增里程碑
          </Button>
        </Flex>
      ) : null}

      {items.length === 0 ? (
        <Empty description="還沒有里程碑" />
      ) : (
        <Flex vertical>
          {items.map((milestone, index) => {
            const late = !milestone.achieved && milestone.dueDate < today;
            const color = milestone.achieved ? token.colorSuccess : late ? token.colorError : token.colorPrimary;
            const progress = percent(milestone.doneTaskCount, milestone.taskCount);
            return (
              <motion.div
                key={milestone.id}
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <Flex gap={16}>
                  {/* 時間軸：圓點 + 往下的線 */}
                  <Flex vertical align="center" style={{ width: 28 }}>
                    <Flex
                      align="center"
                      justify="center"
                      style={{ width: 28, height: 28, borderRadius: '50%', background: color, color: '#fff', flexShrink: 0 }}
                    >
                      {milestone.achieved ? <CheckOutlined /> : <FlagOutlined />}
                    </Flex>
                    {index < items.length - 1 ? <div style={{ flex: 1, width: 2, background: token.colorBorderSecondary }} /> : null}
                  </Flex>
                  <Card size="small" style={{ flex: 1, marginBottom: 16 }}>
                    <Flex vertical gap={8}>
                      <Flex justify="space-between" align="center" wrap gap={8}>
                        <Flex align="center" gap={8} wrap>
                          <Typography.Text strong style={{ fontSize: 15 }}>
                            {milestone.name}
                          </Typography.Text>
                          <Tag color={milestone.achieved ? 'success' : late ? 'error' : 'processing'}>
                            {milestone.dueDate}（{milestone.achieved ? '已達成' : dueText(milestone.dueDate)}）
                          </Tag>
                        </Flex>
                        {access.canManage ? (
                          <Flex align="center" gap={8}>
                            <Switch
                              size="small"
                              checkedChildren="已達成"
                              unCheckedChildren="未達成"
                              checked={milestone.achieved}
                              onChange={(achieved) => {
                                save.mutate({ id: milestone.id, values: { achieved } });
                              }}
                            />
                            <Button
                              type="text"
                              size="small"
                              icon={<EditOutlined />}
                              onClick={() => {
                                setEditing({ mode: 'edit', milestone });
                              }}
                            />
                            <Popconfirm
                              title="刪除這個里程碑？"
                              description="仍有任務掛在底下時無法刪除。"
                              okText="刪除"
                              cancelText="取消"
                              onConfirm={() => {
                                remove.mutate(milestone.id);
                              }}
                            >
                              <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                            </Popconfirm>
                          </Flex>
                        ) : null}
                      </Flex>
                      {hasText(milestone.description) ? (
                        <Typography.Paragraph type="secondary" style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                          {milestone.description}
                        </Typography.Paragraph>
                      ) : null}
                      <Flex align="center" gap={12}>
                        <Progress percent={progress} strokeColor={color} style={{ flex: 1, margin: 0 }} />
                        <Typography.Text type="secondary" style={{ whiteSpace: 'nowrap' }}>
                          {String(milestone.doneTaskCount ?? 0)} / {String(milestone.taskCount ?? 0)} 個任務
                        </Typography.Text>
                      </Flex>
                    </Flex>
                  </Card>
                </Flex>
              </motion.div>
            );
          })}
        </Flex>
      )}

      <FormModal<MilestoneFormValues>
        open={editing !== null}
        title={editing?.mode === 'edit' ? '修改里程碑' : '新增里程碑'}
        initialValues={
          editing?.mode === 'edit'
            ? {
                name: editing.milestone.name,
                description: editing.milestone.description ?? undefined,
                dueDate: dayjs(editing.milestone.dueDate),
              }
            : undefined
        }
        saving={save.isPending}
        onCancel={() => {
          setEditing(null);
        }}
        onSubmit={(values) => {
          save.mutate({
            id: editing?.mode === 'edit' ? editing.milestone.id : undefined,
            values: {
              name: values.name,
              // 清空說明時存成空字串（後端更新時 null 代表不改）
              description: values.description?.trim() ?? '',
              dueDate: values.dueDate.format(DATE_FORMAT),
            },
          });
        }}
      >
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
          <Input autoFocus placeholder="例如 設計定稿" />
        </Form.Item>
        <Form.Item name="dueDate" label="到期日" rules={[{ required: true, message: '請選擇到期日' }]}>
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="description" label="說明" rules={[{ max: 2000 }]}>
          <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
