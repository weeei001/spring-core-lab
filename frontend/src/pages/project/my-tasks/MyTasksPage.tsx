import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Badge, Card, Empty, Flex, Select, Skeleton, Switch, Tag, Typography, theme } from 'antd';
import { AnimatePresence, motion } from 'motion/react';
import dayjs from 'dayjs';
import { projectApi } from '@/api/project/project-api';
import { taskApi } from '@/api/project/task-api';
import type { Task, TaskStatus } from '@/api/project/task.types';
import { DueDate } from '@/components/project/DueDate';
import { PriorityTag } from '@/components/project/Tags';
import { useTaskDrawer } from '@/components/project/hooks';
import { DEFAULT_PROJECT_COLOR, TASK_STATUS_LABELS } from '@/components/project/labels';
import { DATE_FORMAT, isOverdue, isTaskFinished, todayText, weekStart } from '@/components/project/project-utils';
import { TaskDrawer } from '@/components/project/task-drawer/TaskDrawer';
import { useTaskActions } from '@/components/project/useTaskActions';
import { useAuth } from '@/core/auth/useAuth';
import { toOptions } from '@/shared/lib/format';
import { PageTour } from '@/shared/ui/PageTour';
import { MY_TASKS_TOUR } from './tour';

type GroupKey = 'overdue' | 'today' | 'week' | 'later' | 'none' | 'finished';

const GROUPS: { key: GroupKey; label: string; color: string }[] = [
  { key: 'overdue', label: '逾期', color: '#f5222d' },
  { key: 'today', label: '今天到期', color: '#fa8c16' },
  { key: 'week', label: '本週到期', color: '#1677ff' },
  { key: 'later', label: '之後', color: '#8c8c8c' },
  { key: 'none', label: '未設到期日', color: '#bfbfbf' },
  { key: 'finished', label: '已完成', color: '#52c41a' },
];

function groupOf(task: Task, today: string, weekEnd: string): GroupKey {
  if (isTaskFinished(task.status)) {
    return 'finished';
  }
  if (task.dueDate === null) {
    return 'none';
  }
  if (isOverdue(task)) {
    return 'overdue';
  }
  if (task.dueDate === today) {
    return 'today';
  }
  return task.dueDate <= weekEnd ? 'week' : 'later';
}

/** 我的任務：跨專案、指派給我的任務，依到期時間分組；狀態可直接改，點標題打開任務 */
export function MyTasksPage() {
  const { user } = useAuth();
  const { token } = theme.useToken();
  const drawer = useTaskDrawer();
  const { update } = useTaskActions();
  const [showFinished, setShowFinished] = useState(false);

  // 明確給 assigneeId：有 project:admin 的人不受後端限縮，不給會查到所有人的任務
  const myId = String(user.id);
  const tasks = useQuery({
    queryKey: ['task', 'mine', myId, showFinished],
    queryFn: () =>
      taskApi.queryAll(
        showFinished ? { assigneeId: myId } : { assigneeId: myId, statuses: ['TODO', 'IN_PROGRESS', 'REVIEW'] },
      ),
  });
  const projectIds = [...new Set((tasks.data ?? []).map((task) => task.projectId))].sort((a, b) => a - b);
  const projects = useQuery({
    queryKey: ['project', 'by-ids', projectIds],
    queryFn: () => projectApi.query({ ids: projectIds }),
    enabled: projectIds.length > 0,
  });
  const projectOf = (id: number) => projects.data?.find((project) => project.id === id);

  const today = todayText();
  const weekEnd = weekStart(dayjs()).add(6, 'day').format(DATE_FORMAT);
  const grouped = new Map<GroupKey, Task[]>();
  for (const task of tasks.data ?? []) {
    const key = groupOf(task, today, weekEnd);
    grouped.set(key, [...(grouped.get(key) ?? []), task]);
  }
  for (const list of grouped.values()) {
    list.sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999') || a.id - b.id);
  }

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          我的任務
        </Typography.Title>
        <Flex gap={6} align="center" data-tour="my-tasks-finished">
          <Switch size="small" checked={showFinished} onChange={setShowFinished} />
          <Typography.Text>顯示已完成</Typography.Text>
        </Flex>
      </Flex>

      {tasks.isPending ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (tasks.data ?? []).length === 0 ? (
        <Empty description="目前沒有指派給你的任務" />
      ) : (
        <Flex vertical gap={16} data-tour="my-tasks-list">
          {GROUPS.filter((group) => (grouped.get(group.key) ?? []).length > 0).map((group) => {
            const items = grouped.get(group.key) ?? [];
            return (
              <Card
                key={group.key}
                size="small"
                title={
                  <Flex align="center" gap={8}>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', background: group.color }} />
                    {group.label}
                    <Badge count={items.length} color={group.color} />
                  </Flex>
                }
              >
                <Flex vertical>
                  <AnimatePresence initial={false}>
                    {items.map((task) => {
                      const project = projectOf(task.projectId);
                      return (
                        <motion.div key={task.id} layout initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                          <Flex
                            align="center"
                            gap={12}
                            wrap
                            style={{ padding: '8px 0', borderBottom: `1px solid ${token.colorBorderSecondary}` }}
                          >
                            <Select<TaskStatus>
                              size="small"
                              variant="borderless"
                              value={task.status}
                              options={toOptions(TASK_STATUS_LABELS)}
                              style={{ width: 100 }}
                              onChange={(status) => {
                                update.mutate({ id: task.id, values: { status } });
                              }}
                            />
                            <Typography.Link
                              ellipsis
                              style={{ flex: 1, minWidth: 160 }}
                              onClick={() => {
                                drawer.open(task.id);
                              }}
                            >
                              {task.title}
                            </Typography.Link>
                            {project === undefined ? null : (
                              <Tag color={project.color ?? DEFAULT_PROJECT_COLOR} style={{ marginInlineEnd: 0 }}>
                                {project.name}
                              </Tag>
                            )}
                            <PriorityTag priority={task.priority} />
                            <DueDate task={task} />
                          </Flex>
                        </motion.div>
                      );
                    })}
                  </AnimatePresence>
                </Flex>
              </Card>
            );
          })}
        </Flex>
      )}

      <TaskDrawer taskId={drawer.taskId} onClose={drawer.close} onOpen={drawer.open} />
      <PageTour id="my-tasks" steps={MY_TASKS_TOUR} />
    </Flex>
  );
}
