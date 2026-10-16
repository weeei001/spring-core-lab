import { Card, Flex, Tag, Tooltip, Typography, theme } from 'antd';
import { BranchesOutlined, CheckCircleFilled, ClockCircleOutlined } from '@ant-design/icons';
import { motion } from 'motion/react';
import { UserAvatar, type UserDirectory } from '@/modules/basic-data';
import type { Task } from '../../api/task.types';
import { DueDate } from '../../components/DueDate';
import { TASK_PRIORITY_COLORS, TASK_PRIORITY_LABELS } from '../../labels';
import { hoursText } from '../../project-utils';

interface TaskCardProps {
  task: Task;
  directory: UserDirectory;
  /** 拖曳中浮在游標上的那張 */
  overlay?: boolean;
  onOpen?: (taskId: number) => void;
}

/** 看板卡片：左側色條是優先度，底部是到期日、子任務進度、工時與指派人 */
export function TaskCard({ task, directory, overlay = false, onOpen }: TaskCardProps) {
  const { token } = theme.useToken();
  const done = task.status === 'DONE';
  return (
    <motion.div initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.2 }}>
      <Card
        size="small"
        hoverable
        onClick={() => {
          onOpen?.(task.id);
        }}
        style={{
          borderInlineStart: `4px solid ${TASK_PRIORITY_COLORS[task.priority]}`,
          boxShadow: overlay ? token.boxShadowSecondary : undefined,
          transform: overlay ? 'rotate(2deg)' : undefined,
          cursor: overlay ? 'grabbing' : 'grab',
        }}
        styles={{ body: { padding: '10px 12px' } }}
      >
        <Flex vertical gap={6}>
          <Flex justify="space-between" align="center">
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {task.code}
            </Typography.Text>
            <Tooltip title={`優先度：${TASK_PRIORITY_LABELS[task.priority]}`}>
              <span style={{ fontSize: 12, color: TASK_PRIORITY_COLORS[task.priority] }}>{TASK_PRIORITY_LABELS[task.priority]}</span>
            </Tooltip>
          </Flex>
          <Typography.Text delete={task.status === 'CANCELLED'} style={{ fontWeight: 500 }}>
            {done ? (
              <motion.span
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: 'spring', stiffness: 400, damping: 15 }}
                style={{ display: 'inline-block', marginInlineEnd: 6, color: token.colorSuccess }}
              >
                <CheckCircleFilled />
              </motion.span>
            ) : null}
            {task.title}
          </Typography.Text>
          <Flex justify="space-between" align="center" gap={8}>
            <Flex align="center" gap={8} wrap style={{ fontSize: 12 }}>
              <DueDate task={task} />
              {task.subtaskCount !== undefined && task.subtaskCount > 0 ? (
                <Tag icon={<BranchesOutlined />} style={{ marginInlineEnd: 0 }}>
                  {String(task.doneSubtaskCount ?? 0)}/{String(task.subtaskCount)}
                </Tag>
              ) : null}
              {task.loggedHours !== undefined && task.loggedHours > 0 ? (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  <ClockCircleOutlined /> {hoursText(task.loggedHours)}
                </Typography.Text>
              ) : null}
            </Flex>
            {task.assigneeId === null ? null : <UserAvatar userId={task.assigneeId} user={directory.user(task.assigneeId)} />}
          </Flex>
        </Flex>
      </Card>
    </motion.div>
  );
}
