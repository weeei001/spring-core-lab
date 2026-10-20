import { Card, Flex, Tag, Typography, theme } from 'antd';
import { BranchesOutlined, CheckCircleFilled } from '@ant-design/icons';
import type { Task } from '@/api/project/task.types';
import { DueDate } from '@/components/project/DueDate';
import { UserAvatar } from '@/components/user/UserAvatar';
import type { UserDirectory } from '@/components/user/useUserDirectory';

interface TaskCardProps {
  task: Task;
  directory: UserDirectory;
  /** 拖曳中浮在游標上的那張 */
  overlay?: boolean;
  onOpen?: (taskId: number) => void;
}

/** 看板卡片只呈現編號、標題、到期日、子任務進度與負責人。 */
export function TaskCard({ task, directory, overlay = false, onOpen }: TaskCardProps) {
  const { token } = theme.useToken();
  const done = task.status === 'DONE';
  return (
    <Card
      size="small"
      hoverable
      onClick={() => {
        onOpen?.(task.id);
      }}
      style={{
        boxShadow: overlay ? token.boxShadowSecondary : undefined,
        cursor: overlay ? 'grabbing' : 'grab',
      }}
    >
      <Flex vertical gap={6}>
        <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
          {task.code}
        </Typography.Text>
        <Typography.Text delete={task.status === 'CANCELLED'} strong>
          {done ? <CheckCircleFilled style={{ marginInlineEnd: 6, color: token.colorSuccess }} /> : null}
          {task.title}
        </Typography.Text>
        <Flex justify="space-between" align="center" gap={8}>
          <Flex align="center" gap={8} wrap style={{ fontSize: token.fontSizeSM }}>
            <DueDate task={task} />
            {task.subtaskCount !== undefined && task.subtaskCount > 0 ? (
              <Tag icon={<BranchesOutlined />}>
                {String(task.doneSubtaskCount ?? 0)}/{String(task.subtaskCount)}
              </Tag>
            ) : null}
          </Flex>
          {task.assigneeId === null ? null : <UserAvatar userId={task.assigneeId} user={directory.user(task.assigneeId)} />}
        </Flex>
      </Flex>
    </Card>
  );
}
