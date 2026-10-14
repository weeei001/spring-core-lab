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
import { tagApi } from './tag-api';
import type { Tag, TagQuery } from './tag.types';

interface TagFormValues {
  name: string;
}

/** 新增時 tag 為 null */
type Editing = { tag: Tag | null } | null;

/** 標籤維護（查詢需要 product:read，維護需要 tag:write） */
export function TagPage() {
  const { can } = useAuth();
  const writable = can(PERMISSIONS.TAG_WRITE);
  const list = usePagedList<Tag, TagQuery>('tag', tagApi.queryPage, {});
  const { save, remove } = useSaveDelete<Tag>('tag', tagApi);
  const userName = useUserNames(list.rows);
  const [editing, setEditing] = useState<Editing>(null);

  const close = (): void => {
    setEditing(null);
  };

  const columns: TableColumnsType<Tag> = [{ title: '名稱', dataIndex: 'name' }, transColumn<Tag>(userName)];
  if (writable) {
    columns.push({
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_value, tag) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditing({ tag });
            }}
          >
            編輯
          </Button>
          <Popconfirm
            title="確定刪除這個標籤？"
            description="仍有商品使用此標籤時無法刪除。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(tag.id);
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
        標籤
      </Typography.Title>
      <Flex justify="space-between" wrap gap={12}>
        <Input.Search
          placeholder="名稱"
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
              setEditing({ tag: null });
            }}
          >
            新增標籤
          </Button>
        )}
      </Flex>
      <Table<Tag> rowKey="id" columns={columns} dataSource={list.rows} loading={list.loading} pagination={list.pagination} />

      <FormModal<TagFormValues>
        open={editing !== null}
        title={editing?.tag ? '修改標籤' : '新增標籤'}
        initialValues={editing?.tag ? { name: editing.tag.name } : undefined}
        saving={save.isPending}
        onCancel={close}
        onSubmit={(values) => {
          save.mutate({ id: editing?.tag?.id, values: { name: values.name } }, { onSuccess: close });
        }}
      >
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 50 }]}>
          <Input placeholder="例如 新品" />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
