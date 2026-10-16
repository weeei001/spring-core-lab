import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { App, Button, Flex, Popconfirm, Select, Table, Tag, Typography, type TableColumnsType } from 'antd';
import { UserAddOutlined } from '@ant-design/icons';
import { useAuth } from '@/core/auth/useAuth';
import { UserAvatar, UserSelect, type UserDirectory } from '@/modules/basic-data';
import { formatTime, toOptions } from '@/shared/lib/format';
import { memberApi } from '../api/member-api';
import type { ProjectMember, ProjectRole } from '../api/member.types';
import { useInvalidateProjectData, type ProjectAccess } from '../hooks';
import { PROJECT_ROLE_LABELS } from '../labels';

interface MemberTabProps {
  projectId: number;
  members: readonly ProjectMember[];
  directory: UserDirectory;
  access: ProjectAccess;
}

/** 成員：經理可加入、移除成員與調整角色；每個專案至少保留一位經理（後端檢查） */
export function MemberTab({ projectId, members, directory, access }: MemberTabProps) {
  const { user } = useAuth();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const [userId, setUserId] = useState<string | undefined>();
  const [role, setRole] = useState<ProjectRole>('MEMBER');

  const add = useMutation({
    mutationFn: () => memberApi.create({ projectId, userId, role }),
    onSuccess: () => {
      void message.success('已加入成員');
      setUserId(undefined);
    },
    onSettled: invalidate,
  });
  const changeRole = useMutation({
    mutationFn: ({ id, value }: { id: number; value: ProjectRole }) => memberApi.update(id, { role: value }),
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => memberApi.remove(id),
    onSuccess: () => {
      void message.success('已移除成員');
    },
    onSettled: invalidate,
  });

  const sorted = [...members].sort((a, b) => (a.role === b.role ? a.id - b.id : a.role === 'MANAGER' ? -1 : 1));
  const columns: TableColumnsType<ProjectMember> = [
    {
      title: '成員',
      key: 'user',
      render: (_value, member) => (
        <Flex align="center" gap={10}>
          <UserAvatar userId={member.userId} user={directory.user(member.userId)} size="default" />
          <Flex vertical>
            <Typography.Text strong>
              {directory.name(member.userId)}
              {member.userId === String(user.id) ? <Tag style={{ marginInlineStart: 8 }}>我</Tag> : null}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {directory.user(member.userId)?.email ?? ''}
            </Typography.Text>
          </Flex>
        </Flex>
      ),
    },
    {
      title: '角色',
      key: 'role',
      width: 140,
      render: (_value, member) =>
        access.canManage ? (
          <Select<ProjectRole>
            size="small"
            value={member.role}
            options={toOptions(PROJECT_ROLE_LABELS)}
            style={{ width: 100 }}
            onChange={(value) => {
              changeRole.mutate({ id: member.id, value });
            }}
          />
        ) : (
          <Tag color={member.role === 'MANAGER' ? 'blue' : undefined}>{PROJECT_ROLE_LABELS[member.role]}</Tag>
        ),
    },
    { title: '加入時間', key: 'createTime', width: 170, render: (_value, member) => formatTime(member.createTime) },
    {
      title: '操作',
      key: 'actions',
      width: 90,
      render: (_value, member) =>
        access.canManage ? (
          <Popconfirm
            title="移除這位成員？"
            description="指派給該成員的未完成任務會改為未指派。"
            okText="移除"
            cancelText="取消"
            onConfirm={() => {
              remove.mutate(member.id);
            }}
          >
            <Button type="link" size="small" danger>
              移除
            </Button>
          </Popconfirm>
        ) : null,
    },
  ];

  return (
    <Flex vertical gap={16}>
      {access.canManage ? (
        <Flex gap={8} wrap align="center">
          <div style={{ width: 300 }}>
            <UserSelect value={userId} onChange={setUserId} excludeIds={members.map((member) => member.userId)} />
          </div>
          <Select<ProjectRole> value={role} options={toOptions(PROJECT_ROLE_LABELS)} style={{ width: 100 }} onChange={setRole} />
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            disabled={userId === undefined}
            loading={add.isPending}
            onClick={() => {
              add.mutate();
            }}
          >
            加入
          </Button>
        </Flex>
      ) : null}
      <Table<ProjectMember> rowKey="id" columns={columns} dataSource={sorted} pagination={false} />
    </Flex>
  );
}
