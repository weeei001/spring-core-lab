import { Avatar, Flex, Popover, Typography } from 'antd';
import type { User } from './user.types';

const COLORS = ['#1677ff', '#13c2c2', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#2f54eb', '#faad14'];

/** 同一個人永遠同一個顏色 */
function colorOf(id: string): string {
  let hash = 0;
  for (const character of id) {
    hash = (hash * 31 + character.charCodeAt(0)) % 997;
  }
  return COLORS[hash % COLORS.length] ?? '#1677ff';
}

interface UserAvatarProps {
  userId: string;
  /** 由 useUserDirectory 查回；還沒查到時顯示 id */
  user: User | undefined;
  size?: number | 'small' | 'default' | 'large';
}

/** 使用者頭像（名字第一個字），滑過顯示名字與 Email */
export function UserAvatar({ userId, user, size = 'small' }: UserAvatarProps) {
  const name = user?.name ?? userId;
  return (
    <Popover
      content={
        <Flex vertical>
          <Typography.Text strong>{name}</Typography.Text>
          {user?.email == null ? null : <Typography.Text type="secondary">{user.email}</Typography.Text>}
        </Flex>
      }
    >
      <Avatar size={size} style={{ backgroundColor: colorOf(userId), flexShrink: 0, cursor: 'default' }}>
        {name.slice(0, 1)}
      </Avatar>
    </Popover>
  );
}
