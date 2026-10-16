import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, Checkbox, Flex, Form, Input, Popconfirm, Table, Tag, Typography, type TableColumnsType } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { roleApi } from '@/api/basic-data/role-api';
import type { Permission, Role, RoleQuery } from '@/api/basic-data/role.types';
import { useUserNames } from '@/components/user/useUserNames';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { useSaveDelete } from '@/shared/hooks/useSaveDelete';
import { blankToUndefined } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';
import { transColumn } from '@/shared/ui/auditColumn';

interface RoleFormValues {
  code: string;
  name: string;
  permissionCodes?: string[];
}

type Editing = { mode: 'create' } | { mode: 'edit'; role: Role } | null;

/** 依 group 分組，維持後端回傳的順序 */
function groupPermissions(permissions: readonly Permission[]): [string, Permission[]][] {
  const groups = new Map<string, Permission[]>();
  for (const permission of permissions) {
    const items = groups.get(permission.group) ?? [];
    items.push(permission);
    groups.set(permission.group, items);
  }
  return [...groups.entries()];
}

/** 角色維護：把功能權限組合成角色（需要 role:manage，由路由檢查） */
export function RolePage() {
  const list = usePagedList<Role, RoleQuery>('role', roleApi.queryPage, {});
  const { save, remove } = useSaveDelete<Role>('role', roleApi);
  const userName = useUserNames(list.rows);
  const permissions = useQuery({ queryKey: ['permissions'], queryFn: roleApi.permissions, staleTime: Infinity });
  const [editing, setEditing] = useState<Editing>(null);

  const allPermissions = permissions.data ?? [];
  const descriptions = new Map(allPermissions.map((permission) => [permission.code, permission.description] as const));
  const close = (): void => {
    setEditing(null);
  };

  const submit = (values: RoleFormValues): void => {
    if (editing === null) {
      return;
    }
    const permissionCodes = values.permissionCodes ?? [];
    if (editing.mode === 'create') {
      save.mutate({ values: { code: values.code, name: values.name, permissionCodes } }, { onSuccess: close });
    } else {
      // code 建立後不可修改，不送
      save.mutate({ id: editing.role.id, values: { name: values.name, permissionCodes } }, { onSuccess: close });
    }
  };

  const columns: TableColumnsType<Role> = [
    { title: '代碼', dataIndex: 'code', width: 160 },
    { title: '名稱', dataIndex: 'name', width: 160 },
    {
      title: '功能權限',
      key: 'permissions',
      render: (_value, role) => (
        <Flex wrap gap={4}>
          {role.permissionCodes.map((code) => (
            <Tag key={code}>{descriptions.get(code) ?? code}</Tag>
          ))}
        </Flex>
      ),
    },
    transColumn<Role>(userName),
    {
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_value, role) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditing({ mode: 'edit', role });
            }}
          >
            編輯
          </Button>
          <Popconfirm
            title="確定刪除這個角色？"
            description="仍有使用者擁有此角色時無法刪除。"
            okText="刪除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(role.id);
            }}
          >
            <Button type="link" size="small" danger>
              刪除
            </Button>
          </Popconfirm>
        </Flex>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        角色
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
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditing({ mode: 'create' });
          }}
        >
          新增角色
        </Button>
      </Flex>
      <Table<Role> rowKey="id" columns={columns} dataSource={list.rows} loading={list.loading} pagination={list.pagination} />

      <FormModal<RoleFormValues>
        open={editing !== null}
        title={editing?.mode === 'edit' ? '修改角色' : '新增角色'}
        initialValues={
          editing?.mode === 'edit'
            ? { code: editing.role.code, name: editing.role.name, permissionCodes: editing.role.permissionCodes }
            : { permissionCodes: [] }
        }
        saving={save.isPending}
        onCancel={close}
        onSubmit={submit}
      >
        <Form.Item
          name="code"
          label="代碼"
          extra="建立後不可修改，例如 WAREHOUSE"
          rules={[
            { required: true, message: '請輸入代碼' },
            { pattern: /^[A-Z0-9_]+$/, message: '只能使用大寫英文、數字與底線' },
            { max: 50 },
          ]}
        >
          <Input disabled={editing?.mode === 'edit'} />
        </Form.Item>
        <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
          <Input placeholder="例如 倉管" />
        </Form.Item>
        <Form.Item name="permissionCodes" label="功能權限">
          <Checkbox.Group style={{ width: '100%' }}>
            <Flex vertical gap={12}>
              {groupPermissions(allPermissions).map(([group, items]) => (
                <Flex key={group} vertical gap={4}>
                  <Typography.Text strong>{group}</Typography.Text>
                  <Flex wrap gap={8}>
                    {items.map((permission) => (
                      <Checkbox key={permission.code} value={permission.code}>
                        {permission.description}
                      </Checkbox>
                    ))}
                  </Flex>
                </Flex>
              ))}
            </Flex>
          </Checkbox.Group>
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
