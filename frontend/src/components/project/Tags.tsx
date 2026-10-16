import type { ReactNode } from 'react';
import { Badge, Flex, Tag } from 'antd';
import { BugOutlined, ExperimentOutlined, RocketOutlined, ToolOutlined } from '@ant-design/icons';
import type { ProjectLabel } from '@/api/project/label.types';
import type { ProjectStatus } from '@/api/project/project.types';
import type { TaskPriority, TaskStatus, TaskType } from '@/api/project/task.types';
import { useProjectLabels } from './hooks';
import {
  PROJECT_STATUS_COLORS,
  PROJECT_STATUS_LABELS,
  TASK_PRIORITY_COLORS,
  TASK_PRIORITY_LABELS,
  TASK_STATUS_COLORS,
  TASK_STATUS_LABELS,
  TASK_TYPE_COLORS,
  TASK_TYPE_LABELS,
} from './labels';

export function ProjectStatusTag({ status }: { status: ProjectStatus }) {
  return <Tag color={PROJECT_STATUS_COLORS[status]}>{PROJECT_STATUS_LABELS[status]}</Tag>;
}

export function TaskStatusTag({ status }: { status: TaskStatus }) {
  return <Tag color={TASK_STATUS_COLORS[status]}>{TASK_STATUS_LABELS[status]}</Tag>;
}

/** 優先度：色點 + 文字 */
export function PriorityTag({ priority }: { priority: TaskPriority }) {
  return <Badge color={TASK_PRIORITY_COLORS[priority]} text={TASK_PRIORITY_LABELS[priority]} />;
}

export const TASK_TYPE_ICONS: Record<TaskType, ReactNode> = {
  FEATURE: <RocketOutlined />,
  BUG: <BugOutlined />,
  CHORE: <ToolOutlined />,
  SPIKE: <ExperimentOutlined />,
};

/** 任務類型；iconOnly 用在看板卡片這種空間小的地方 */
export function TaskTypeTag({ type, iconOnly = false }: { type: TaskType; iconOnly?: boolean }) {
  return (
    <Tag color={TASK_TYPE_COLORS[type]} icon={TASK_TYPE_ICONS[type]} title={TASK_TYPE_LABELS[type]}>
      {iconOnly ? null : TASK_TYPE_LABELS[type]}
    </Tag>
  );
}

/** 單一標籤：用標籤自己的顏色 */
export function LabelTag({ label }: { label: Pick<ProjectLabel, 'name' | 'color'> }) {
  return (
    <Tag color={label.color} style={{ marginInlineEnd: 0 }}>
      {label.name}
    </Tag>
  );
}

/** 任務的標籤；標籤資料由專案的標籤快取取得，已刪除的標籤不顯示 */
export function TaskLabelTags({ projectId, labelIds }: { projectId: number; labelIds: readonly number[] }) {
  const labels = useProjectLabels(projectId);
  if (labelIds.length === 0) {
    return null;
  }
  const items = (labels.data ?? []).filter((label) => labelIds.includes(label.id));
  return items.length === 0 ? null : (
    <Flex gap={4} wrap>
      {items.map((label) => (
        <LabelTag key={label.id} label={label} />
      ))}
    </Flex>
  );
}
