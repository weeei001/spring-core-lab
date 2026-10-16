import { DatePicker, Flex, Form, Input, InputNumber, Select } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import type { Project, ProjectStatus } from '@/api/project/project.types';
import { blankToUndefined } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';
import { ColorSwatches } from './ColorSwatches';
import { DEFAULT_PROJECT_COLOR, PROJECT_STATUS_LABELS } from './labels';
import { DATE_FORMAT } from './project-utils';

interface ProjectFormValues {
  name: string;
  description?: string;
  status?: ProjectStatus;
  startDate?: Dayjs | null;
  endDate?: Dayjs | null;
  color: string;
  /** 看板 WIP 上限，空白表示不限 */
  wipInProgress?: number | null;
  wipReview?: number | null;
}

interface ProjectFormModalProps {
  open: boolean;
  /** 有值為修改 */
  project?: Project;
  saving: boolean;
  onCancel: () => void;
  onSubmit: (values: Partial<Project>) => void;
}

/** 新增或修改專案；狀態只在新增時選，之後從專案頁的狀態選單改 */
export function ProjectFormModal({ open, project, saving, onCancel, onSubmit }: ProjectFormModalProps) {
  const creating = project === undefined;
  return (
    <FormModal<ProjectFormValues>
      open={open}
      title={creating ? '新增專案' : '修改專案'}
      initialValues={
        creating
          ? { status: 'PLANNING', color: DEFAULT_PROJECT_COLOR }
          : {
              name: project.name,
              description: project.description ?? undefined,
              startDate: project.startDate === null ? null : dayjs(project.startDate),
              endDate: project.endDate === null ? null : dayjs(project.endDate),
              color: project.color ?? DEFAULT_PROJECT_COLOR,
              wipInProgress: project.wipLimits.IN_PROGRESS ?? null,
              wipReview: project.wipLimits.REVIEW ?? null,
            }
      }
      saving={saving}
      onCancel={onCancel}
      onSubmit={(values) => {
        onSubmit({
          name: values.name,
          // 清空說明時存成空字串（後端更新時 null 代表不改）
          description: creating ? blankToUndefined(values.description) : (values.description?.trim() ?? ''),
          status: creating ? values.status : undefined,
          startDate: values.startDate?.format(DATE_FORMAT),
          endDate: values.endDate?.format(DATE_FORMAT),
          color: values.color,
          // 一律整份送出，空白的欄位就是取消上限
          wipLimits: {
            ...(values.wipInProgress == null ? {} : { IN_PROGRESS: values.wipInProgress }),
            ...(values.wipReview == null ? {} : { REVIEW: values.wipReview }),
          },
        });
      }}
    >
      <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
        <Input autoFocus placeholder="例如 官網改版" />
      </Form.Item>
      <Form.Item name="description" label="說明" rules={[{ max: 2000 }]}>
        <Input.TextArea autoSize={{ minRows: 2, maxRows: 6 }} />
      </Form.Item>
      {creating ? (
        <Form.Item name="status" label="狀態">
          <Select
            options={(['PLANNING', 'ACTIVE'] as const).map((status) => ({ value: status, label: PROJECT_STATUS_LABELS[status] }))}
          />
        </Form.Item>
      ) : null}
      <Flex gap={12}>
        <Form.Item name="startDate" label="開始日" style={{ flex: 1 }}>
          {/* 後端更新時 null 代表不改，已設定的日期只能改不能清空 */}
          <DatePicker allowClear={creating} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="endDate"
          label="結束日"
          style={{ flex: 1 }}
          dependencies={['startDate']}
          rules={[
            ({ getFieldValue }) => ({
              validator: (_rule, value: Dayjs | null | undefined) => {
                const start = getFieldValue('startDate') as Dayjs | null | undefined;
                return value == null || start == null || !start.isAfter(value, 'day')
                  ? Promise.resolve()
                  : Promise.reject(new Error('結束日不可早於開始日'));
              },
            }),
          ]}
        >
          <DatePicker allowClear={creating} style={{ width: '100%' }} />
        </Form.Item>
      </Flex>
      <Flex gap={12}>
        <Form.Item name="wipInProgress" label="WIP 上限：進行中" tooltip="看板「進行中」最多幾張，超過時亮紅燈；空白為不限" style={{ flex: 1 }}>
          <InputNumber min={1} max={99} precision={0} placeholder="不限" style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="wipReview" label="WIP 上限：審核中" tooltip="Code review 堆太多時提醒大家先消化；空白為不限" style={{ flex: 1 }}>
          <InputNumber min={1} max={99} precision={0} placeholder="不限" style={{ width: '100%' }} />
        </Form.Item>
      </Flex>
      <Form.Item name="color" label="顏色">
        <ColorSwatches />
      </Form.Item>
    </FormModal>
  );
}
