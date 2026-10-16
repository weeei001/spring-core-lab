import { Card, Flex, Progress, Tag, Tooltip, Typography, theme } from 'antd';
import { CalendarOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { motion } from 'motion/react';
import { DEFAULT_PROJECT_COLOR, PROJECT_ROLE_LABELS } from '../labels';
import type { ProjectRole } from '../member/member.types';
import { hasText, percent } from '../project-utils';
import { ProjectStatusTag } from '../ui/Tags';
import type { Project } from './project.types';

interface ProjectCardProps {
  project: Project;
  myRole: ProjectRole | undefined;
  /** 第幾張，用來錯開進場動畫 */
  index: number;
  onOpen: (projectId: number) => void;
}

/** 專案卡片：頂部色條、狀態、進度條、任務與逾期數；滑過浮起 */
export function ProjectCard({ project, myRole, index, onOpen }: ProjectCardProps) {
  const { token } = theme.useToken();
  const color = project.color ?? DEFAULT_PROJECT_COLOR;
  const progress = percent(project.doneTaskCount, project.taskCount);
  const overdue = project.overdueTaskCount ?? 0;
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      // 進場的延遲只放在 animate，滑過浮起才不會跟著延遲
      animate={{ opacity: 1, y: 0, transition: { duration: 0.3, delay: Math.min(index, 12) * 0.04 } }}
      whileHover={{ y: -4, transition: { duration: 0.15 } }}
      style={{ height: '100%' }}
    >
      <Card
        hoverable
        onClick={() => {
          onOpen(project.id);
        }}
        style={{ height: '100%', borderTop: `4px solid ${color}` }}
        styles={{ body: { padding: 16, height: '100%' } }}
      >
        <Flex vertical gap={10} style={{ height: '100%' }}>
          <Flex justify="space-between" align="start" gap={8}>
            <Flex vertical style={{ minWidth: 0 }}>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {project.code}
              </Typography.Text>
              <Typography.Text strong ellipsis style={{ fontSize: 16 }}>
                {project.name}
              </Typography.Text>
            </Flex>
            <ProjectStatusTag status={project.status} />
          </Flex>
          <Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ margin: 0, minHeight: 44 }}>
            {hasText(project.description) ? project.description : '沒有說明'}
          </Typography.Paragraph>
          <Progress percent={progress} strokeColor={color} size="small" />
          <Flex justify="space-between" align="center" wrap gap={8} style={{ marginTop: 'auto' }}>
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
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  <CalendarOutlined /> {project.endDate}
                </Typography.Text>
              )}
              {myRole === undefined ? null : (
                <Tag color={myRole === 'MANAGER' ? token.colorPrimary : undefined} style={{ marginInlineEnd: 0 }}>
                  {PROJECT_ROLE_LABELS[myRole]}
                </Tag>
              )}
            </Flex>
          </Flex>
        </Flex>
      </Card>
    </motion.div>
  );
}
