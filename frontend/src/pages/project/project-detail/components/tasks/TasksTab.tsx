import { useState } from 'react';
import { Button, Flex, Input, Segmented, Select, Skeleton, Switch, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router';
import type { TaskStatus } from '@/api/project/task.types';
import { useProjectLabels, useProjectTasks, type ProjectAccess } from '@/components/project/hooks';
import { useTaskActions } from '@/components/project/useTaskActions';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { useAuth } from '@/core/auth/useAuth';
import { PageTour } from '@/shared/ui/PageTour';
import { TaskBoardView } from './TaskBoardView';
import { TaskFormModal } from './TaskFormModal';
import { TaskTableView } from './TaskTableView';
import { TASKS_TOUR } from './tour';

/** 指派人篩選的特殊值 */
const ME = '__me__';
const NONE = '__none__';

interface TasksTabProps {
  projectId: number;
  /** 專案的看板 WIP 上限 */
  wipLimits: Partial<Record<TaskStatus, number>>;
  access: ProjectAccess;
  directory: UserDirectory;
  memberOptions: { value: string; label: string }[];
  onOpen: (taskId: number) => void;
}

/** 兩種檢視共用 URL 篩選，完整任務頁返回後仍可接著查看。 */
export function TasksTab({ projectId, wipLimits, access, directory, memberOptions, onOpen }: TasksTabProps) {
  const { user } = useAuth();
  const tasks = useProjectTasks(projectId);
  const labels = useProjectLabels(projectId);
  const { create } = useTaskActions();
  const [params, setParams] = useSearchParams();
  const view = params.get('view') === 'table' ? 'table' : 'board';
  const keywordInput = params.get('q') ?? '';
  const assignee = params.get('assignee') || undefined;
  const showCancelled = params.get('cancelled') === '1';
  const [creating, setCreating] = useState<TaskStatus | null>(null);

  const changeFilter = (key: string, value: string | undefined): void => {
    setParams((current) => {
      const next = new URLSearchParams(current);
      if (value === undefined || value === '') {
        next.delete(key);
      } else {
        next.set(key, value);
      }
      next.delete('taskPage');
      return next;
    }, { replace: true, preventScrollReset: true });
  };

  if (tasks.isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />;
  }

  const allTasks = tasks.data ?? [];
  const keyword = keywordInput.trim().toLowerCase();
  const myId = String(user.id);
  const filtered = allTasks.filter((task) => {
    if (!showCancelled && task.status === 'CANCELLED') {
      return false;
    }
    if (keyword !== '' && !`${task.code} ${task.title}`.toLowerCase().includes(keyword)) {
      return false;
    }
    if (assignee === ME && task.assigneeId !== myId) {
      return false;
    }
    if (assignee === NONE && task.assigneeId !== null) {
      return false;
    }
    if (assignee !== undefined && assignee !== ME && assignee !== NONE && task.assigneeId !== assignee) {
      return false;
    }
    return true;
  });

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Flex gap={8} wrap align="center" data-tour="task-filters">
          <Input.Search
            aria-label="編號或標題"
            placeholder="編號或標題"
            allowClear
            style={{ width: 200 }}
            value={keywordInput}
            onChange={(event) => {
              changeFilter('q', event.target.value);
            }}
          />
          <Select<string>
            aria-label="負責人篩選"
            allowClear
            placeholder="全部負責人"
            style={{ width: 160 }}
            value={assignee}
            options={[{ value: ME, label: '指派給我' }, { value: NONE, label: '未指派' }, ...memberOptions]}
            showSearch={{ optionFilterProp: 'label' }}
            onChange={(assignee: string | undefined) => {
              changeFilter('assignee', assignee);
            }}
          />
          <Flex gap={6} align="center">
            <Switch
              aria-label="顯示已取消"
              size="small"
              checked={showCancelled}
              onChange={(checked) => {
                changeFilter('cancelled', checked ? '1' : undefined);
              }}
            />
            <Typography.Text>顯示已取消</Typography.Text>
          </Flex>
        </Flex>
        <Flex gap={8} align="center" wrap>
          <Segmented
            aria-label="任務檢視"
            value={view}
            options={[{ value: 'board', label: '看板' }, { value: 'table', label: '表格' }]}
            onChange={(value) => {
              setParams((current) => {
                const next = new URLSearchParams(current);
                next.set('view', value);
                return next;
              }, { preventScrollReset: true });
            }}
          />
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
            editable={access.canEditTasks}
            onOpen={onOpen}
          />
        ) : (
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
        )}
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
