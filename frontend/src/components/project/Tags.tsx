import { Tag } from 'antd';
import type { ProjectStatus } from '@/api/project/project.types';
import type { TaskPriority, TaskStatus } from '@/api/project/task.types';
import { PROJECT_STATUS_COLORS, PROJECT_STATUS_LABELS, TASK_PRIORITY_COLORS, TASK_PRIORITY_LABELS, TASK_STATUS_COLORS, TASK_STATUS_LABELS } from './labels';

export function ProjectStatusTag({ status }: { status: ProjectStatus }) {
  return <Tag color={PROJECT_STATUS_COLORS[status]}>{PROJECT_STATUS_LABELS[status]}</Tag>;
}

export function TaskStatusTag({ status }: { status: TaskStatus }) {
  return <Tag color={TASK_STATUS_COLORS[status]}>{TASK_STATUS_LABELS[status]}</Tag>;
}

/** 優先度：小色點 + 文字 */
export function PriorityTag({ priority }: { priority: TaskPriority }) {
  return (
    <Tag>
      <span
        style={{
          display: 'inline-block',
          width: 8,
          height: 8,
          borderRadius: '50%',
          marginInlineEnd: 6,
          background: TASK_PRIORITY_COLORS[priority],
        }}
      />
      {TASK_PRIORITY_LABELS[priority]}
    </Tag>
  );
}
