import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { App, Button, Flex, Input, Segmented, Select, Skeleton, Switch, Typography } from 'antd';
import { BarsOutlined, PlusOutlined, ProjectOutlined, TableOutlined } from '@ant-design/icons';
import { useAuth } from '@/core/auth/useAuth';
import type { UserDirectory } from '@/modules/basic-data';
import { toOptions } from '@/shared/lib/format';
import { PageTour } from '@/shared/ui/PageTour';
import { TASKS_TOUR } from '../guide/tours';
import { useInvalidateProjectData, useProjectMilestones, useProjectTasks, type ProjectAccess } from '../hooks';
import { TASK_PRIORITY_LABELS } from '../labels';
import { milestoneApi } from '../milestone/milestone-api';
import type { Milestone } from '../milestone/milestone.types';
import { TaskBoardView } from './TaskBoardView';
import { TaskFormModal } from './TaskFormModal';
import { TaskGanttView } from './TaskGanttView';
import { TaskTableView } from './TaskTableView';
import type { Task, TaskPriority, TaskStatus } from './task.types';
import { useTaskActions } from './useTaskActions';

type ViewMode = 'table' | 'board' | 'gantt';

const VIEW_KEY = 'project-task-view';
/** 指派人篩選的特殊值 */
const ME = '__me__';
const NONE = '__none__';

function loadView(): ViewMode {
  try {
    const value = localStorage.getItem(VIEW_KEY);
    return value === 'table' || value === 'gantt' ? value : 'board';
  } catch {
    return 'board';
  }
}

function saveView(view: ViewMode): void {
  try {
    localStorage.setItem(VIEW_KEY, view);
  } catch {
    // 存不了就用預設值
  }
}

interface Filters {
  keyword: string;
  /** user id，或 ME、NONE；undefined 表示全部 */
  assignee?: string;
  priorities: TaskPriority[];
  milestoneId?: number;
}

interface TasksTabProps {
  projectId: number;
  access: ProjectAccess;
  directory: UserDirectory;
  memberOptions: { value: string; label: string }[];
  onOpen: (taskId: number) => void;
}

/** 三種檢視共用同一份任務資料與篩選條件；篩選在前端做，切換檢視不必重查 */
export function TasksTab({ projectId, access, directory, memberOptions, onOpen }: TasksTabProps) {
  const { user } = useAuth();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const tasks = useProjectTasks(projectId);
  const milestones = useProjectMilestones(projectId);
  const { create, update } = useTaskActions();
  const [view, setView] = useState<ViewMode>(loadView);
  const [filters, setFilters] = useState<Filters>({ keyword: '', priorities: [] });
  const [showCancelled, setShowCancelled] = useState(false);
  const [creating, setCreating] = useState<TaskStatus | null>(null);

  const rescheduleMilestone = useMutation({
    mutationFn: ({ milestone, dueDate }: { milestone: Milestone; dueDate: string }) => milestoneApi.update(milestone.id, { dueDate }),
    onSuccess: () => {
      void message.success('已調整里程碑日期');
    },
    onSettled: invalidate,
  });

  if (tasks.isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  const allTasks = tasks.data ?? [];
  const keyword = filters.keyword.trim().toLowerCase();
  const myId = String(user.id);
  const filtered = allTasks.filter((task) => {
    if (keyword !== '' && !`${task.code} ${task.title}`.toLowerCase().includes(keyword)) {
      return false;
    }
    if (filters.assignee === ME && task.assigneeId !== myId) {
      return false;
    }
    if (filters.assignee === NONE && task.assigneeId !== null) {
      return false;
    }
    if (filters.assignee !== undefined && filters.assignee !== ME && filters.assignee !== NONE && task.assigneeId !== filters.assignee) {
      return false;
    }
    if (filters.priorities.length > 0 && !filters.priorities.includes(task.priority)) {
      return false;
    }
    if (filters.milestoneId !== undefined && task.milestoneId !== filters.milestoneId) {
      return false;
    }
    return !(view !== 'board' && !showCancelled && task.status === 'CANCELLED');
  });

  const reschedule = (task: Task, startDate: string, dueDate: string): Promise<unknown> =>
    task.startDate === startDate && task.dueDate === dueDate
      ? Promise.resolve()
      : update.mutateAsync({ id: task.id, values: { startDate, dueDate } });

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Flex gap={8} wrap align="center" data-tour="task-filters">
          <Input.Search
            placeholder="編號或標題"
            allowClear
            style={{ width: 200 }}
            onChange={(event) => {
              setFilters({ ...filters, keyword: event.target.value });
            }}
          />
          <Select<string>
            allowClear
            placeholder="全部指派人"
            style={{ width: 160 }}
            value={filters.assignee}
            options={[{ value: ME, label: '指派給我' }, { value: NONE, label: '未指派' }, ...memberOptions]}
            showSearch={{ optionFilterProp: 'label' }}
            onChange={(assignee: string | undefined) => {
              setFilters({ ...filters, assignee });
            }}
          />
          <Select<TaskPriority[]>
            mode="multiple"
            allowClear
            placeholder="全部優先度"
            style={{ minWidth: 150 }}
            maxTagCount="responsive"
            value={filters.priorities}
            options={toOptions(TASK_PRIORITY_LABELS)}
            onChange={(priorities) => {
              setFilters({ ...filters, priorities });
            }}
          />
          <Select<number>
            allowClear
            placeholder="全部里程碑"
            style={{ width: 160 }}
            value={filters.milestoneId}
            options={(milestones.data ?? []).map((milestone) => ({ value: milestone.id, label: milestone.name }))}
            onChange={(milestoneId: number | undefined) => {
              setFilters({ ...filters, milestoneId });
            }}
          />
          <Flex gap={6} align="center">
            <Switch size="small" checked={showCancelled} onChange={setShowCancelled} />
            <Typography.Text>顯示已取消</Typography.Text>
          </Flex>
        </Flex>
        <Flex gap={8} align="center">
          <div data-tour="task-views">
            <Segmented<ViewMode>
              value={view}
              onChange={(next) => {
                setView(next);
                saveView(next);
              }}
              options={[
                { value: 'table', icon: <TableOutlined />, label: '表格' },
                { value: 'board', icon: <ProjectOutlined />, label: '看板' },
                { value: 'gantt', icon: <BarsOutlined />, label: '甘特' },
              ]}
            />
          </div>
          {access.canEditTasks ? (
            <Button
              type="primary"
              data-tour="task-create"
              icon={<PlusOutlined />}
              onClick={() => {
                setCreating('TODO');
              }}
            >
              新增任務
            </Button>
          ) : null}
        </Flex>
      </Flex>

      <div data-tour="task-content">
        {view === 'table' ? (
          <TaskTableView
            tasks={filtered}
            allTasks={allTasks}
            directory={directory}
            memberOptions={memberOptions}
            milestones={milestones.data ?? []}
            editable={access.canEditTasks}
            onOpen={onOpen}
          />
        ) : view === 'board' ? (
          <TaskBoardView
            tasks={filtered}
            allTasks={allTasks}
            directory={directory}
            editable={access.canEditTasks}
            showCancelled={showCancelled}
            onOpen={onOpen}
            onQuickAdd={(status, title) => {
              create.mutate({ projectId, status, title });
            }}
          />
        ) : (
          <TaskGanttView
            tasks={filtered}
            milestones={filters.milestoneId === undefined ? (milestones.data ?? []) : (milestones.data ?? []).filter((m) => m.id === filters.milestoneId)}
            editable={access.canEditTasks}
            milestoneEditable={access.canManage}
            onOpen={onOpen}
            onReschedule={reschedule}
            onMilestoneReschedule={(milestone, dueDate) =>
              milestone.dueDate === dueDate ? Promise.resolve() : rescheduleMilestone.mutateAsync({ milestone, dueDate })
            }
          />
        )}
      </div>

      <TaskFormModal
        open={creating !== null}
        projectId={projectId}
        defaultStatus={creating ?? 'TODO'}
        memberOptions={memberOptions}
        milestones={milestones.data ?? []}
        saving={create.isPending}
        onCancel={() => {
          setCreating(null);
        }}
        onSubmit={(values) => {
          create.mutate(values, {
            onSuccess: () => {
              setCreating(null);
            },
          });
        }}
      />
      {/* 第一次打開任務分頁時播放；重看教學時已併在專案頁的導覽裡 */}
      <PageTour id="project-tasks" steps={TASKS_TOUR} replayable={false} />
    </Flex>
  );
}
