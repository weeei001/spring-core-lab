import { useState } from 'react';
import { Checkbox, Empty, Flex, Input, Progress, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { Task } from '@/api/project/task.types';
import { UserAvatar } from '@/components/user/UserAvatar';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { DueDate } from '../DueDate';
import { TaskStatusTag } from '../Tags';
import { percent } from '../project-utils';
import { useTaskActions } from '../useTaskActions';

interface TaskSubtasksProps {
  parent: Task;
  /** 同專案全部任務 */
  allTasks: readonly Task[];
  directory: UserDirectory;
  editable: boolean;
  onOpen: (taskId: number) => void;
}

/** 子任務勾選清單：勾選即完成，取消勾選回到待辦；底部輸入標題按 Enter 新增 */
export function TaskSubtasks({ parent, allTasks, directory, editable, onOpen }: TaskSubtasksProps) {
  const { create, update } = useTaskActions();
  const [title, setTitle] = useState('');
  const subtasks = allTasks
    .filter((task) => task.parentId === parent.id && task.status !== 'CANCELLED')
    .sort((a, b) => a.id - b.id);
  const done = subtasks.filter((task) => task.status === 'DONE').length;

  return (
    <Flex vertical gap={12}>
      {subtasks.length > 0 ? (
        <Progress percent={percent(done, subtasks.length)} format={() => `${String(done)}/${String(subtasks.length)}`} />
      ) : null}
      {subtasks.length === 0 && !editable ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="沒有子任務" /> : null}
      <Flex vertical gap={4}>
          {subtasks.map((task) => (
            <Flex key={task.id} align="center" gap={8} style={{ padding: '4px 0' }}>
              <Checkbox
                checked={task.status === 'DONE'}
                disabled={!editable}
                onChange={(event) => {
                  update.mutate({ id: task.id, values: { status: event.target.checked ? 'DONE' : 'TODO' } });
                }}
              />
              <Typography.Link
                delete={task.status === 'DONE'}
                style={{ flex: 1 }}
                ellipsis
                onClick={() => {
                  onOpen(task.id);
                }}
              >
                {task.title}
              </Typography.Link>
              <DueDate task={task} />
              <TaskStatusTag status={task.status} />
              {task.assigneeId === null ? null : <UserAvatar userId={task.assigneeId} user={directory.user(task.assigneeId)} />}
            </Flex>
          ))}
      </Flex>
      {editable ? (
        <Input
          prefix={<PlusOutlined />}
          placeholder="新增子任務，按 Enter"
          value={title}
          disabled={create.isPending}
          onChange={(event) => {
            setTitle(event.target.value);
          }}
          onPressEnter={() => {
            const trimmed = title.trim();
            if (trimmed === '') {
              return;
            }
            create.mutate(
              { projectId: parent.projectId, parentId: parent.id, milestoneId: parent.milestoneId, title: trimmed },
              {
                onSuccess: () => {
                  setTitle('');
                },
              },
            );
          }}
        />
      ) : null}
    </Flex>
  );
}
