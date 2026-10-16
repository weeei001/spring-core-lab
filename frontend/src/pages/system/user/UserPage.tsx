import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Flex, Form, Input, Select, Table, Tag, Typography, message, type TableColumnsType } from 'antd';
import { roleApi } from '@/api/basic-data/role-api';
import { userApi } from '@/api/basic-data/user-api';
import type { User, UserQuery } from '@/api/basic-data/user.types';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { blankToUndefined, formatTime } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';

interface RoleFormValues {
  roleIds: number[];
}

interface UpdateRolesInput {
  id: number;
  roleIds: number[];
}

/** 使用者列表與指派角色（需要 role:manage，由路由檢查）；使用者由第一次登入自動建立 */
export function UserPage() {
  const queryClient = useQueryClient();
  const list = usePagedList<User, UserQuery>('user', userApi.queryPage, {});
  const roles = useQuery({ queryKey: ['role', 'options'], queryFn: () => roleApi.query({}) });
  const [editing, setEditing] = useState<User | null>(null);

  const roleOptions = (roles.data ?? []).map((role) => ({ value: role.id, label: role.name }));
  const roleNames = new Map((roles.data ?? []).map((role) => [role.id, role.name] as const));

  const updateRoles = useMutation({
    mutationFn: ({ id, roleIds }: UpdateRolesInput) => userApi.updateRoles(id, roleIds),
    onSuccess: () => {
      void message.success('已更新角色');
      setEditing(null);
      // 改的可能是自己，權限也要重新取得
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: ['user'] }),
        queryClient.invalidateQueries({ queryKey: ['me'] }),
      ]);
    },
  });

  const columns: TableColumnsType<User> = [
    { title: '名稱', dataIndex: 'name' },
    { title: '帳號', dataIndex: 'username' },
    { title: 'Email', dataIndex: 'email' },
    {
      title: '角色',
      key: 'roles',
      render: (_value, user) => (
        <Flex wrap gap={4}>
          {user.roleIds.map((id) => (
            <Tag key={id}>{roleNames.get(id) ?? String(id)}</Tag>
          ))}
        </Flex>
      ),
    },
    { title: '第一次登入', key: 'createTime', width: 160, render: (_value, user) => formatTime(user.createTime) },
    {
      title: '操作',
      key: 'actions',
      width: 110,
      render: (_value, user) => (
        <Button
          type="link"
          size="small"
          onClick={() => {
            setEditing(user);
          }}
        >
          指派角色
        </Button>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        使用者
      </Typography.Title>
      <Input.Search
        placeholder="名稱、帳號或 Email"
        allowClear
        style={{ width: 280 }}
        onSearch={(value) => {
          list.search({ keyword: blankToUndefined(value) });
        }}
      />
      <Table<User> rowKey="id" columns={columns} dataSource={list.rows} loading={list.loading} pagination={list.pagination} />

      <FormModal<RoleFormValues>
        open={editing !== null}
        title={`指派角色：${editing?.name ?? ''}`}
        initialValues={{ roleIds: editing?.roleIds ?? [] }}
        saving={updateRoles.isPending}
        onCancel={() => {
          setEditing(null);
        }}
        onSubmit={(values) => {
          if (editing !== null) {
            updateRoles.mutate({ id: editing.id, roleIds: values.roleIds });
          }
        }}
      >
        <Form.Item name="roleIds" label="角色" extra="整批取代；調整後最晚 1 分鐘生效，不用重新登入">
          <Select mode="multiple" options={roleOptions} placeholder="選擇角色" />
        </Form.Item>
      </FormModal>
    </Flex>
  );
}
