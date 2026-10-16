import { useState } from 'react';
import { Button, Flex, Form, Input, InputNumber, Popconfirm, Select, Table, Typography, type TableColumnsType } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { serialNumberApi } from '@/api/basic-data/serial-number-api';
import type { ResetCycle, SerialNumber, SerialNumberQuery } from '@/api/basic-data/serial-number.types';
import { useUserNames } from '@/components/user/useUserNames';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { useSaveDelete } from '@/shared/hooks/useSaveDelete';
import { blankToUndefined, toOptions } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';
import { transColumn } from '@/shared/ui/auditColumn';
import { RESET_CYCLE_LABELS } from './labels';

interface SerialNumberFormValues {
  serialKey: string;
  name: string;
  rule: string;
  resetCycle: ResetCycle;
  /** 只有新增時可以指定起始值 */
  lastIndex?: number | null;
}

type Editing = { mode: 'create' } | { mode: 'edit'; serialNumber: SerialNumber } | null;

/** 編號規則維護（查詢需要 serial-number:read，維護需要 serial-number:write） */
export function SerialNumberPage() {
  const { can } = useAuth();
  const writable = can(PERMISSIONS.SERIAL_NUMBER_WRITE);
  const list = usePagedList<SerialNumber, SerialNumberQuery>('serial-number', serialNumberApi.queryPage, {});
  const { save, remove } = useSaveDelete<SerialNumber>('serial-number', serialNumberApi);
  const userName = useUserNames(list.rows);
  const [editing, setEditing] = useState<Editing>(null);

  const close = (): void => {
    setEditing(null);
  };

  const submit = (values: SerialNumberFormValues): void => {
    if (editing === null) {
      return;
    }
    const common = { name: values.name, rule: values.rule, resetCycle: values.resetCycle };
    if (editing.mode === 'create') {
      save.mutate(
        { values: { ...common, serialKey: values.serialKey, lastIndex: values.lastIndex ?? undefined } },
        { onSuccess: close },
      );
    } else {
      // serialKey 不可修改、lastIndex 由取號維護，都不送
      save.mutate({ id: editing.serialNumber.id, values: common }, { onSuccess: close });
    }
  };

  const columns: TableColumnsType<SerialNumber> = [
    { title: '代碼', dataIndex: 'serialKey', width: 160 },
    { title: '名稱', dataIndex: 'name' },
    { title: '編號格式', dataIndex: 'rule' },
    { title: '最後流水號', dataIndex: 'lastIndex', align: 'right', width: 110 },
    { title: '歸零週期', key: 'resetCycle', width: 100, render: (_value, row) => RESET_CYCLE_LABELS[row.resetCycle] },
    transColumn<SerialNumber>(userName),
  ];
  if (writable) {
    columns.push({
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_value, row) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditing({ mode: 'edit', serialNumber: row });
            }}
          >
            編輯
          </Button>
          <Popconfirm
            title="確定刪除這個編號規則？"
            description="刪除後無法再取號，代碼也不能再使用。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(row.id);
            }}
          >
            <Button type="link" size="small" danger>
              刪除
            </Button>
          </Popconfirm>
        </Flex>
      ),
    });
  }

  const creating = editing?.mode !== 'edit';
  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4}>
        編號規則
      </Typography.Title>
      <Flex justify="space-between" wrap gap={12}>
        <Input.Search
          placeholder="代碼或名稱"
          allowClear
          style={{ width: 240 }}
          onSearch={(value) => {
            list.search({ keyword: blankToUndefined(value) });
          }}
        />
        {writable && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing({ mode: 'create' });
            }}
          >
            新增規則
          </Button>
        )}
      </Flex>
      <Table<SerialNumber>
        rowKey="id"
        columns={columns}
        dataSource={list.rows}
        loading={list.loading}
        pagination={list.pagination}
      />

      <FormModal<SerialNumberFormValues>
        open={editing !== null}
        title={creating ? '新增編號規則' : '修改編號規則'}
        initialValues={
          editing?.mode === 'edit'
            ? {
                serialKey: editing.serialNumber.serialKey,
                name: editing.serialNumber.name,
                rule: editing.serialNumber.rule,
                resetCycle: editing.serialNumber.resetCycle,
              }
            : { resetCycle: 'NONE' }
        }
        saving={save.isPending}
        onCancel={close}
        onSubmit={submit}
      >
        <Form.Item
          name="serialKey"
          label="代碼"
          extra="取號時指定，建立後不可修改，例如 ORDER"
          rules={[
            { required: true, message: '請輸入代碼' },
            { pattern: /^[A-Z0-9_]+$/, message: '只能使用大寫英文、數字與底線' },
            { max: 50 },
          ]}
        >
          <Input disabled={!creating} />
        </Form.Item>
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
          <Input />
        </Form.Item>
        <Form.Item
          name="rule"
          label="編號格式"
          extra="{0000} 為補零流水號（只能一個），{yyyyMM} 等為日期，例如 P{yyyyMM}{0000} 產生 P2026100001"
          rules={[{ required: true, message: '請輸入編號格式' }, { max: 50 }]}
        >
          <Input />
        </Form.Item>
        <Form.Item name="resetCycle" label="歸零週期" rules={[{ required: true }]}>
          <Select options={toOptions(RESET_CYCLE_LABELS)} />
        </Form.Item>
        {creating && (
          <Form.Item name="lastIndex" label="起始值" extra="第一次取號為起始值 + 1；不填為 0">
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
        )}
      </FormModal>
    </Flex>
  );
}
