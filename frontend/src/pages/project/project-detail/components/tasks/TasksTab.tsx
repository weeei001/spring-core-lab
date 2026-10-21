import { useState } from 'react';
import { Button, Flex, Input, Select, Skeleton, Switch, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { TaskStatus } from '@/api/project/task.types';
import { useProjectLabels, useProjectTasks, type ProjectAccess } from '@/components/project/hooks';
import { useTaskActions } from '@/components/project/useTaskActions';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { useAuth } from '@/core/auth/useAuth';
import { PageTour } from '@/shared/ui/PageTour';
import { TaskBoardView } from './TaskBoardView';
import { TaskFormModal } from './TaskFormModal';
import { TASKS_TOUR } from './tour';

/** 指派人篩選的特殊值 */
const ME = '__me__';
const NONE = '__none__';

interface Filters {
  keyword: string;
  /** user id，或 ME、NONE；undefined 表示全部 */
  assignee?: string;
}

interface TasksTabProps {
  projectId: number;
  /** 專案的看板 WIP 上限 */
  wipLimits: Partial<Record<TaskStatus, number>>;
  access: ProjectAccess;
  directory: UserDirectory;
  memberOptions: { value: string; label: string }[];
  onOpen: (taskId: number) => void;
}

/** ponytail: 雛形用單一看板，先讓成員能新增、指派與完成任務。 */
export function TasksTab({ projectId, wipLimits, access, directory, memberOptions, onOpen }: TasksTabProps) {
  const { user } = useAuth();
  const tasks = useProjectTasks(projectId);
  const labels = useProjectLabels(projectId);
  const { create } = useTaskActions();
  const [filters, setFilters] = useState<Filters>({ keyword: '' });
  const [showCancelled, setShowCancelled] = useState(false);
  const [creating, setCreating] = useState<TaskStatus | null>(null);

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
    return true;
  });

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
            placeholder="全部負責人"
            style={{ width: 160 }}
            value={filters.assignee}
            options={[{ value: ME, label: '指派給我' }, { value: NONE, label: '未指派' }, ...memberOptions]}
            showSearch={{ optionFilterProp: 'label' }}
            onChange={(assignee: string | undefined) => {
              setFilters({ ...filters, assignee });
            }}
          />
          <Flex gap={6} align="center">
            <Switch aria-label="顯示已取消" size="small" checked={showCancelled} onChange={setShowCancelled} />
            <Typography.Text>顯示已取消</Typography.Text>
          </Flex>
        </Flex>
        <Flex gap={8} align="center">
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
        <TaskBoardView
          tasks={filtered}
          allTasks={allTasks}
          directory={directory}
          editable={access.canEditTasks}
          showCancelled={showCancelled}
          wipLimits={wipLimits}
          onOpen={onOpen}
          onQuickAdd={(status, title) => {
            create.mutate({ projectId, status, title });
          }}
        />
      </div>

      <TaskFormModal
        open={creating !== null}
        projectId={projectId}
        defaultStatus={creating ?? 'TODO'}
        memberOptions={memberOptions}
        labels={labels.data ?? []}
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
