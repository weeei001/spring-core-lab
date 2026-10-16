import { useQuery } from '@tanstack/react-query';
import { Button, Card, Col, Empty, Flex, Row, Skeleton, Tag, Typography } from 'antd';
import { ClockCircleOutlined, FieldTimeOutlined, FlagOutlined, ProjectOutlined, UnorderedListOutlined, WarningOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { motion } from 'motion/react';
import { useNavigate } from 'react-router';
import { memberApi } from '@/api/project/member-api';
import { milestoneApi } from '@/api/project/milestone-api';
import { projectApi } from '@/api/project/project-api';
import type { ProjectStatus } from '@/api/project/project.types';
import { taskApi } from '@/api/project/task-api';
import { workLogApi } from '@/api/project/worklog-api';
import { DueDate } from '@/components/project/DueDate';
import { ProjectCard } from '@/components/project/ProjectCard';
import { PriorityTag } from '@/components/project/Tags';
import { useTaskDrawer } from '@/components/project/hooks';
import { DEFAULT_PROJECT_COLOR } from '@/components/project/labels';
import { DATE_FORMAT, dueText, hoursText, isOverdue, todayText, weekStart } from '@/components/project/project-utils';
import { TaskDrawer } from '@/components/project/task-drawer/TaskDrawer';
import { useAuth } from '@/core/auth/useAuth';
import { BarChart } from '@/shared/ui/BarChart';
import { PageTour } from '@/shared/ui/PageTour';
import { StatTile } from '@/shared/ui/StatTile';
import { DASHBOARD_TOUR } from './tour';

const OPEN_STATUSES: ProjectStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD'];
const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];

function greeting(): string {
  const hour = dayjs().hour();
  if (hour < 11) {
    return '早安';
  }
  return hour < 18 ? '午安' : '晚安';
}

/** 儀表板：我的待辦與逾期、本週工時、參與中的專案、即將到期的里程碑 */
export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const drawer = useTaskDrawer();
  const myId = String(user.id);
  const today = todayText();
  const monday = weekStart(dayjs());
  const days = Array.from({ length: 7 }, (_, index) => monday.add(index, 'day').format(DATE_FORMAT));
  const sunday = monday.add(6, 'day').format(DATE_FORMAT);

  // 都明確帶自己的 id：有 project:admin 的人不受後端限縮
  const projects = useQuery({
    queryKey: ['project', 'dashboard', myId],
    queryFn: () => projectApi.query({ memberUserId: myId, statuses: OPEN_STATUSES }),
  });
  const tasks = useQuery({
    queryKey: ['task', 'mine', myId, false],
    queryFn: () => taskApi.queryAll({ assigneeId: myId, statuses: ['TODO', 'IN_PROGRESS', 'REVIEW'] }),
  });
  const hours = useQuery({
    queryKey: ['work-log', 'summary', 'mine', myId, days[0]],
    queryFn: () => workLogApi.summary({ userId: myId, fromDate: monday.format(DATE_FORMAT), toDate: sunday }),
  });
  const projectIds = (projects.data ?? []).map((project) => project.id);
  const roles = useQuery({
    queryKey: ['project-member', 'mine', projectIds],
    queryFn: () => memberApi.query({ projectIds, userId: myId }),
    enabled: projectIds.length > 0,
  });
  const milestones = useQuery({
    queryKey: ['milestone', 'dashboard', projectIds],
    queryFn: () =>
      milestoneApi.query({ projectIds, achieved: false, toDueDate: dayjs().add(14, 'day').format(DATE_FORMAT) }),
    enabled: projectIds.length > 0,
    select: (items) => [...items].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
  });

  const myTasks = tasks.data ?? [];
  const overdue = myTasks.filter(isOverdue);
  const dueSoon = myTasks
    .filter((task) => task.dueDate !== null && task.dueDate <= dayjs().add(7, 'day').format(DATE_FORMAT))
    .sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))
    .slice(0, 8);
  const hoursByDay = new Map<string, number>();
  for (const item of hours.data ?? []) {
    hoursByDay.set(item.workDate, (hoursByDay.get(item.workDate) ?? 0) + item.hours);
  }
  const weekHours = [...hoursByDay.values()].reduce((sum, value) => sum + value, 0);
  const projectName = (id: number) => projects.data?.find((project) => project.id === id);

  return (
    <Flex vertical gap={20}>
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <Typography.Title level={3} style={{ margin: 0 }}>
          {greeting()}，{user.name}
        </Typography.Title>
        <Typography.Text type="secondary">{dayjs().format('YYYY 年 M 月 D 日 dddd')}</Typography.Text>
      </motion.div>

      <Flex gap={16} wrap data-tour="dashboard-stats">
        <StatTile
          title="我的待辦"
          value={myTasks.length}
          icon={<UnorderedListOutlined />}
          color="#1677ff"
          onClick={() => {
            void navigate('/my-tasks');
          }}
        />
        <StatTile
          title="逾期"
          value={overdue.length}
          icon={<WarningOutlined />}
          color="#f5222d"
          delay={0.05}
          onClick={() => {
            void navigate('/my-tasks');
          }}
        />
        <StatTile
          title="本週工時"
          value={weekHours}
          decimals={1}
          suffix="h"
          icon={<FieldTimeOutlined />}
          color="#13c2c2"
          delay={0.1}
          onClick={() => {
            void navigate('/timesheet');
          }}
        />
        <StatTile
          title="參與中的專案"
          value={projectIds.length}
          icon={<ProjectOutlined />}
          color="#722ed1"
          delay={0.15}
          onClick={() => {
            void navigate('/projects');
          }}
        />
      </Flex>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card
            size="small"
            title={
              <span>
                <ClockCircleOutlined /> 一週內到期
              </span>
            }
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => {
                  void navigate('/my-tasks');
                }}
              >
                全部
              </Button>
            }
            style={{ height: '100%' }}
            data-tour="dashboard-due"
          >
            {tasks.isPending ? (
              <Skeleton active />
            ) : dueSoon.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="一週內沒有到期的任務" />
            ) : (
              <Flex vertical gap={10}>
                {dueSoon.map((task, index) => {
                  const project = projectName(task.projectId);
                  return (
                    <motion.div key={task.id} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: index * 0.04 }}>
                      <Flex align="center" gap={8}>
                        <PriorityTag priority={task.priority} />
                        <Typography.Link
                          ellipsis
                          style={{ flex: 1 }}
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
                        <DueDate task={task} />
                      </Flex>
                    </motion.div>
                  );
                })}
              </Flex>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card size="small" title={`本週工時 ${hoursText(weekHours)}`} style={{ height: '100%' }} data-tour="dashboard-hours">
            {hours.isPending ? (
              <Skeleton active />
            ) : (
              <BarChart
                height={150}
                format={hoursText}
                data={days.map((date, index) => ({
                  key: date,
                  label: `週${WEEKDAYS[index] ?? ''}`,
                  value: hoursByDay.get(date) ?? 0,
                  tooltip: `${dayjs(date).format('MM/DD')}：${hoursText(hoursByDay.get(date) ?? 0)}`,
                  highlight: date === today,
                }))}
              />
            )}
          </Card>
        </Col>
      </Row>

      <Flex vertical gap={12}>
        <Flex justify="space-between" align="center">
          <Typography.Title level={5} style={{ margin: 0 }}>
            參與中的專案
          </Typography.Title>
          <Button
            type="link"
            onClick={() => {
              void navigate('/projects');
            }}
          >
            全部專案
          </Button>
        </Flex>
        {projects.isPending ? (
          <Skeleton active />
        ) : (projects.data ?? []).length === 0 ? (
          <Empty description="還沒有參與任何專案" />
        ) : (
          <Row gutter={[16, 16]}>
            {(projects.data ?? []).slice(0, 6).map((project, index) => (
              <Col key={project.id} xs={24} sm={12} xl={8}>
                <ProjectCard
                  project={project}
                  index={index}
                  myRole={roles.data?.find((member) => member.projectId === project.id)?.role}
                  onOpen={(id) => {
                    void navigate(`/projects/${String(id)}`);
                  }}
                />
              </Col>
            ))}
          </Row>
        )}
      </Flex>

      <Card
        size="small"
        title={
          <span>
            <FlagOutlined /> 兩週內到期的里程碑
          </span>
        }
      >
        {(milestones.data ?? []).length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="兩週內沒有到期的里程碑" />
        ) : (
          <Flex vertical gap={10}>
            {(milestones.data ?? []).map((milestone) => {
              const project = projectName(milestone.projectId);
              return (
                <Flex key={milestone.id} align="center" gap={8} wrap>
                  <Typography.Text strong style={{ flex: 1 }}>
                    {milestone.name}
                  </Typography.Text>
                  {project === undefined ? null : (
                    <Tag
                      color={project.color ?? DEFAULT_PROJECT_COLOR}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        void navigate(`/projects/${String(project.id)}?tab=milestones`);
                      }}
                    >
                      {project.name}
                    </Tag>
                  )}
                  <Typography.Text type={milestone.dueDate < today ? 'danger' : 'secondary'}>
                    {milestone.dueDate}（{dueText(milestone.dueDate)}）
                  </Typography.Text>
                </Flex>
              );
            })}
          </Flex>
        )}
      </Card>

      <TaskDrawer taskId={drawer.taskId} onClose={drawer.close} onOpen={drawer.open} />
      <PageTour id="dashboard" steps={DASHBOARD_TOUR} />
    </Flex>
  );
}
