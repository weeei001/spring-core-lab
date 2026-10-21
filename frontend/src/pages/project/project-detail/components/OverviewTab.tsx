import { Avatar, Card, Col, Empty, Flex, Progress, Row, Typography } from 'antd';
import type { ProjectMember } from '@/api/project/member.types';
import type { Project } from '@/api/project/project.types';
import { DueDate } from '@/components/project/DueDate';
import { TaskStatusTag } from '@/components/project/Tags';
import { useProjectTasks } from '@/components/project/hooks';
import { PROJECT_ROLE_LABELS, TASK_STATUSES, TASK_STATUS_COLORS, TASK_STATUS_LABELS } from '@/components/project/labels';
import { hasText, isOverdue, isTaskFinished, percent } from '@/components/project/project-utils';
import { UserAvatar } from '@/components/user/UserAvatar';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { useAuth } from '@/core/auth/useAuth';
import { StackedBar } from '@/shared/ui/StackedBar';
import { StatTile } from '@/shared/ui/StatTile';

interface OverviewTabProps {
  project: Project;
  members: readonly ProjectMember[];
  directory: UserDirectory;
  onOpen: (taskId: number) => void;
}

/** 專案概覽：數字卡片、任務狀態分布、我的待辦、成員 */
export function OverviewTab({ project, members, directory, onOpen }: OverviewTabProps) {
  const { user } = useAuth();
  const tasks = useProjectTasks(project.id);

  // 只算主任務，與後端的專案進度一致
  const mainTasks = (tasks.data ?? []).filter((task) => task.parentId === null);
  const counted = mainTasks.filter((task) => task.status !== 'CANCELLED');
  const done = counted.filter((task) => task.status === 'DONE').length;
  const overdue = counted.filter(isOverdue).length;
  const progress = percent(done, counted.length);
  const mine = (tasks.data ?? [])
    .filter((task) => task.assigneeId === String(user.id) && !isTaskFinished(task.status))
    .sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'))
    .slice(0, 6);

  return (
    <Flex vertical gap={16}>
      <Flex gap={16} wrap>
        <StatTile title="主任務" value={counted.length} />
        <StatTile title="已完成" value={done} />
        <StatTile title="逾期" value={overdue} />
      </Flex>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Flex vertical gap={16}>
            <Card size="small" title="進度">
              <Flex align="center" gap={24} wrap>
                <Progress type="circle" percent={progress} size={110} />
                <Flex vertical gap={12} style={{ flex: 1, minWidth: 240 }}>
                  <Typography.Text type="secondary">
                    主任務 {String(done)} / {String(counted.length)} 完成（取消的不計）
                  </Typography.Text>
                  <StackedBar
                    segments={TASK_STATUSES.map((status) => ({
                      key: status,
                      label: TASK_STATUS_LABELS[status],
                      value: mainTasks.filter((task) => task.status === status).length,
                      color: TASK_STATUS_COLORS[status],
                    }))}
                  />
                </Flex>
              </Flex>
            </Card>
            <Card size="small" title="我在這個專案的待辦">
              {mine.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="沒有指派給你的未完成任務" />
              ) : (
                <Flex vertical gap={8}>
                  {mine.map((task) => (
                    <Flex key={task.id} align="center" gap={8}>
                      <TaskStatusTag status={task.status} />
                      <Typography.Link
                        ellipsis
                        style={{ flex: 1 }}
                        onClick={() => {
                          onOpen(task.id);
                        }}
                      >
                        {task.title}
                      </Typography.Link>
                      <DueDate task={task} />
                    </Flex>
                  ))}
                </Flex>
              )}
            </Card>
            {hasText(project.description) ? (
              <Card size="small" title="說明">
                <Typography.Paragraph style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{project.description}</Typography.Paragraph>
              </Card>
            ) : null}
          </Flex>
        </Col>
        <Col xs={24} lg={8}>
          <Flex vertical gap={16}>
            <Card size="small" title={`成員（${String(members.length)}）`}>
              <Flex vertical gap={10}>
                <Avatar.Group max={{ count: 8 }}>
                  {members.map((member) => (
                    <UserAvatar key={member.id} userId={member.userId} user={directory.user(member.userId)} size="default" />
                  ))}
                </Avatar.Group>
                <Typography.Text type="secondary">
                  經理：
                  {members
                    .filter((member) => member.role === 'MANAGER')
                    .map((member) => directory.name(member.userId))
                    .join('、') || '—'}
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {PROJECT_ROLE_LABELS.MANAGER}可維護專案資料與成員
                </Typography.Text>
              </Flex>
            </Card>
          </Flex>
        </Col>
      </Row>
    </Flex>
  );
}
