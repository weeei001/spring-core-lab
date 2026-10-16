import { DatePicker, Flex, Form, Input, InputNumber, Select } from 'antd';
import type { Dayjs } from 'dayjs';
import { FormModal } from '@/shared/ui/FormModal';
import { toOptions } from '@/shared/lib/format';
import { TASK_PRIORITY_LABELS, TASK_STATUS_LABELS } from '../labels';
import type { Milestone } from '../milestone/milestone.types';
import { DATE_FORMAT } from '../project-utils';
import type { Task, TaskPriority, TaskStatus } from './task.types';

interface TaskFormValues {
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId?: string;
  milestoneId?: number;
  /** DatePicker 清除後是 null */
  startDate?: Dayjs | null;
  dueDate?: Dayjs | null;
  estimateHours?: number;
}

interface TaskFormModalProps {
  open: boolean;
  projectId: number;
  /** 有值時建立的是子任務 */
  parent?: Task;
  /** 預設狀態，例如在看板某一欄新增 */
  defaultStatus?: TaskStatus;
  memberOptions: { value: string; label: string }[];
  milestones: readonly Milestone[];
  saving: boolean;
  onCancel: () => void;
  onSubmit: (values: Partial<Task>) => void;
}

/** 新增任務（或子任務）；建立後的修改都在任務抽屜裡直接改 */
export function TaskFormModal({
  open,
  projectId,
  parent,
  defaultStatus = 'TODO',
  memberOptions,
  milestones,
  saving,
  onCancel,
  onSubmit,
}: TaskFormModalProps) {
  return (
    <FormModal<TaskFormValues>
      open={open}
      title={parent === undefined ? '新增任務' : `新增子任務：${parent.title}`}
      initialValues={{ status: defaultStatus, priority: 'MEDIUM', milestoneId: parent?.milestoneId ?? undefined }}
      saving={saving}
      onCancel={onCancel}
      onSubmit={(values) => {
        onSubmit({
          projectId,
          parentId: parent?.id ?? null,
          title: values.title,
          description: values.description,
          status: values.status,
          priority: values.priority,
          assigneeId: values.assigneeId,
          milestoneId: values.milestoneId,
          startDate: values.startDate?.format(DATE_FORMAT),
          dueDate: values.dueDate?.format(DATE_FORMAT),
          estimateHours: values.estimateHours,
        });
      }}
    >
      <Form.Item name="title" label="標題" rules={[{ required: true, whitespace: true, message: '請輸入標題' }, { max: 200 }]}>
        <Input autoFocus placeholder="例如 首頁版型設計" />
      </Form.Item>
      <Form.Item name="description" label="說明" rules={[{ max: 5000 }]}>
        <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} />
      </Form.Item>
      <Flex gap={12}>
        <Form.Item name="status" label="狀態" style={{ flex: 1 }}>
          <Select options={toOptions(TASK_STATUS_LABELS)} />
        </Form.Item>
        <Form.Item name="priority" label="優先度" style={{ flex: 1 }}>
          <Select options={toOptions(TASK_PRIORITY_LABELS)} />
        </Form.Item>
      </Flex>
      <Flex gap={12}>
        <Form.Item name="assigneeId" label="指派人" style={{ flex: 1 }}>
          <Select allowClear options={memberOptions} placeholder="未指派" showSearch={{ optionFilterProp: 'label' }} />
        </Form.Item>
        <Form.Item name="milestoneId" label="里程碑" style={{ flex: 1 }}>
          <Select
            allowClear
            placeholder="不屬於里程碑"
            options={milestones.map((milestone) => ({ value: milestone.id, label: milestone.name }))}
          />
        </Form.Item>
      </Flex>
      <Flex gap={12}>
        <Form.Item name="startDate" label="開始日" style={{ flex: 1 }}>
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="dueDate"
          label="到期日"
          style={{ flex: 1 }}
          dependencies={['startDate']}
          rules={[
            ({ getFieldValue }) => ({
              validator: (_rule, value: Dayjs | null | undefined) => {
                const start = getFieldValue('startDate') as Dayjs | null | undefined;
                return value == null || start == null || !start.isAfter(value, 'day')
                  ? Promise.resolve()
                  : Promise.reject(new Error('到期日不可早於開始日'));
              },
            }),
          ]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="estimateHours" label="預估工時" style={{ flex: 1 }}>
          <InputNumber min={0} max={99999} step={0.5} precision={1} suffix="h" style={{ width: '100%' }} />
        </Form.Item>
      </Flex>
    </FormModal>
  );
}
