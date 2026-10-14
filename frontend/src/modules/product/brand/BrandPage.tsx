import { useState } from 'react';
import { Button, Flex, Form, Input, Popconfirm, Table, Typography, type TableColumnsType } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { useUserNames } from '@/modules/basic-data';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { useSaveDelete } from '@/shared/hooks/useSaveDelete';
import { blankToUndefined } from '@/shared/lib/format';
import { transColumn } from '@/shared/ui/auditColumn';
import { FormModal } from '@/shared/ui/FormModal';
import { brandApi } from './brand-api';
import type { Brand, BrandQuery } from './brand.types';

interface BrandFormValues {
  code: string;
  name: string;
}

type Editing = { mode: 'create' } | { mode: 'edit'; brand: Brand } | null;

/** 品牌維護（查詢需要 product:read，維護需要 brand:write） */
export function BrandPage() {
  const { can } = useAuth();
  const writable = can(PERMISSIONS.BRAND_WRITE);
  const list = usePagedList<Brand, BrandQuery>('brand', brandApi.queryPage, {});
  const { save, remove } = useSaveDelete<Brand>('brand', brandApi);
  const userName = useUserNames(list.rows);
  const [editing, setEditing] = useState<Editing>(null);

  const close = (): void => {
    setEditing(null);
  };

  const submit = (values: BrandFormValues): void => {
    if (editing === null) {
      return;
    }
    if (editing.mode === 'create') {
      save.mutate({ values: { code: values.code, name: values.name } }, { onSuccess: close });
    } else {
      // code 建立後不可修改，不送
      save.mutate({ id: editing.brand.id, values: { name: values.name } }, { onSuccess: close });
    }
  };

  const columns: TableColumnsType<Brand> = [
    { title: '代碼', dataIndex: 'code', width: 200 },
    { title: '名稱', dataIndex: 'name' },
    transColumn<Brand>(userName),
  ];
  if (writable) {
    columns.push({
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_value, brand) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditing({ mode: 'edit', brand });
            }}
          >
            編輯
          </Button>
          <Popconfirm
            title="確定刪除這個品牌？"
            description="仍有商品使用此品牌時無法刪除；刪除後代碼不能再使用。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(brand.id);
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

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        品牌
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
            新增品牌
          </Button>
        )}
      </Flex>
      <Table<Brand> rowKey="id" columns={columns} dataSource={list.rows} loading={list.loading} pagination={list.pagination} />

      <FormModal<BrandFormValues>
        open={editing !== null}
        title={editing?.mode === 'edit' ? '修改品牌' : '新增品牌'}
        initialValues={editing?.mode === 'edit' ? { code: editing.brand.code, name: editing.brand.name } : undefined}
        saving={save.isPending}
        onCancel={close}
        onSubmit={submit}
      >
        <Form.Item
          name="code"
          label="代碼"
          extra="商品以此關聯，建立後不可修改，例如 APPLE"
          rules={[
            { required: true, message: '請輸入代碼' },
            { pattern: /^[A-Z0-9_]+$/, message: '只能使用大寫英文、數字與底線' },
            { max: 50 },
          ]}
        >
          <Input disabled={editing?.mode === 'edit'} />
        </Form.Item>
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
          <Input />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
