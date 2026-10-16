import { Card, Flex, Progress, Tag, Tooltip, Typography, theme } from 'antd';
import { CalendarOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import type { ProjectRole } from '@/api/project/member.types';
import type { Project } from '@/api/project/project.types';
import { ProjectStatusTag } from './Tags';
import { PROJECT_ROLE_LABELS } from './labels';
import { hasText, percent } from './project-utils';

interface ProjectCardProps {
  project: Project;
  myRole: ProjectRole | undefined;
  onOpen: (projectId: number) => void;
}

/** 專案卡片：編號、名稱、狀態、說明、進度條、任務與逾期數 */
export function ProjectCard({ project, myRole, onOpen }: ProjectCardProps) {
  const { token } = theme.useToken();
  const progress = percent(project.doneTaskCount, project.taskCount);
  const overdue = project.overdueTaskCount ?? 0;
  return (
    <Card
      hoverable
      onClick={() => {
        onOpen(project.id);
      }}
      style={{ height: '100%' }}
      styles={{ body: { height: '100%' } }}
    >
      <Flex vertical gap={12} style={{ height: '100%' }}>
        <Flex justify="space-between" align="start" gap={8}>
          <Flex vertical style={{ minWidth: 0 }}>
            <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
              {project.code}
            </Typography.Text>
            <Typography.Text strong ellipsis style={{ fontSize: token.fontSizeLG }}>
              {project.name}
            </Typography.Text>
          </Flex>
          <ProjectStatusTag status={project.status} />
        </Flex>
        {hasText(project.description) ? (
          <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ margin: 0 }}>
            {project.description}
          </Typography.Paragraph>
        ) : null}
        <Flex vertical gap={8} style={{ marginTop: 'auto' }}>
          <Progress percent={progress} size="small" />
          <Flex justify="space-between" align="center" wrap gap={8}>
            <Flex gap={12} align="center">
              <Tooltip title="完成 / 全部主任務">
                <Typography.Text type="secondary">
                  <CheckCircleOutlined /> {String(project.doneTaskCount ?? 0)}/{String(project.taskCount ?? 0)}
                </Typography.Text>
              </Tooltip>
              {overdue > 0 ? (
                <Tooltip title="逾期任務">
                  <Typography.Text type="danger">
                    <WarningOutlined /> {String(overdue)}
                  </Typography.Text>
                </Tooltip>
              ) : null}
            </Flex>
            <Flex gap={8} align="center">
              {project.endDate === null ? null : (
                <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
                  <CalendarOutlined /> {project.endDate}
                </Typography.Text>
              )}
              {myRole === undefined ? null : <Tag>{PROJECT_ROLE_LABELS[myRole]}</Tag>}
            </Flex>
          </Flex>
        </Flex>
      </Flex>
    </Card>
  );
}
