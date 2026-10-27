import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { App, Breadcrumb, Button, Card, Dropdown, Flex, Popconfirm, Progress, Result, Skeleton, Tabs, Typography } from 'antd';
import { CalendarOutlined, DeleteOutlined, DownOutlined, EditOutlined } from '@ant-design/icons';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { projectApi } from '@/api/project/project-api';
import type { Project, ProjectStatus } from '@/api/project/project.types';
import { ProjectFormModal } from '@/components/project/ProjectFormModal';
import { ProjectStatusTag } from '@/components/project/Tags';
import { useInvalidateProjectData, useProjectContext, useTaskDrawer } from '@/components/project/hooks';
import { PROJECT_STATUS_LABELS, PROJECT_STATUS_NEXT } from '@/components/project/labels';
import { percent } from '@/components/project/project-utils';
import { TaskDrawer } from '@/components/project/task-drawer/TaskDrawer';
import { useMemberDirectory } from '@/components/project/useMemberDirectory';
import { PageTour } from '@/shared/ui/PageTour';
import { MemberTab } from './components/MemberTab';
import { TasksTab } from './components/tasks/TasksTab';
import { PROJECT_DETAIL_TOUR } from './tour';

// ponytail: 雛形先完成任務協作；進階管理頁等實際試用有需求再接回。
const TABS = ['tasks', 'members'] as const;
type TabKey = (typeof TABS)[number];

function isTab(value: string | null): value is TabKey {
  return value !== null && (TABS as readonly string[]).includes(value);
}

/** 專案詳情：上方是專案資訊與狀態，下方分頁；目前分頁記在網址的 ?tab= */
export function ProjectDetailPage() {
  const params = useParams();
  const projectId = Number(params['id']);
  if (!Number.isInteger(projectId) || projectId <= 0) {
    return <Result status="404" title="找不到專案" />;
  }
  return <ProjectDetail key={projectId} projectId={projectId} />;
}

function ProjectDetail({ projectId }: { projectId: number }) {
  const navigate = useNavigate();
  const { message, modal } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const [searchParams, setSearchParams] = useSearchParams();
  const drawer = useTaskDrawer();
  const { project, members, access, loading, notFound } = useProjectContext(projectId);
  const { directory, options: memberOptions } = useMemberDirectory(members);
  const [editing, setEditing] = useState(false);

  const tabParam = searchParams.get('tab');
  const tab: TabKey = isTab(tabParam) ? tabParam : 'tasks';

  const update = useMutation({
    mutationFn: (values: Partial<Project>) => projectApi.update(projectId, values),
    onSuccess: () => {
      void message.success('已更新專案');
      setEditing(false);
    },
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: () => projectApi.remove(projectId),
    onSuccess: () => {
      void message.success('已刪除專案');
      void navigate('/projects');
    },
    onSettled: invalidate,
  });

  if (loading) {
    return <Skeleton active paragraph={{ rows: 10 }} />;
  }
  if (notFound || project === undefined) {
    return <Result status="404" title="找不到專案" subTitle="專案可能已刪除，或你不是這個專案的成員。" extra={<Link to="/projects">回專案列表</Link>} />;
  }

  const changeStatus = (status: ProjectStatus): void => {
    const run = (): void => {
      update.mutate({ status });
    };
    if (status === 'DONE' || status === 'CANCELLED') {
      modal.confirm({
        title: `確定將專案改為「${PROJECT_STATUS_LABELS[status]}」？`,
        content: '結案或取消後，專案底下的任務、成員、工時都會變成唯讀，直到重新開啟。',
        okText: '確定',
        cancelText: '取消',
        onOk: run,
      });
    } else {
      run();
    }
  };

  return (
    <Flex vertical gap={16} style={{ minWidth: 0 }}>
      <Breadcrumb items={[{ title: <Link to="/projects">專案</Link> }, { title: project.name }]} />
      <Card data-tour="project-header">
        <Flex justify="space-between" align="center" wrap gap={16}>
          <Flex vertical gap={6} style={{ minWidth: 0 }}>
            <Flex align="center" gap={10} wrap>
              <Typography.Title level={3}>
                {project.name}
              </Typography.Title>
              {access.canChangeStatus ? (
                <Dropdown
                  menu={{
                    items: PROJECT_STATUS_NEXT[project.status].map((status) => ({
                      key: status,
                      label: status === 'ACTIVE' && access.closed ? '重新開啟' : `改為${PROJECT_STATUS_LABELS[status]}`,
                    })),
                    onClick: ({ key }) => {
                      changeStatus(key as ProjectStatus);
                    },
                  }}
                >
                  <Button size="small" type="text" data-tour="project-status">
                    <ProjectStatusTag status={project.status} />
                    <DownOutlined />
                  </Button>
                </Dropdown>
              ) : (
                <ProjectStatusTag status={project.status} />
              )}
            </Flex>
            <Flex gap={16} wrap>
              <Typography.Text type="secondary">{project.code}</Typography.Text>
              <Typography.Text type="secondary">
                <CalendarOutlined /> {project.startDate ?? '未定'} ～ {project.endDate ?? '未定'}
              </Typography.Text>
            </Flex>
          </Flex>
          <Flex align="center" gap={16}>
            <Progress type="circle" size={64} percent={percent(project.doneTaskCount, project.taskCount)} />
            {access.canManage ? (
              <Flex gap={8}>
                <Button
                  icon={<EditOutlined />}
                  onClick={() => {
                    setEditing(true);
                  }}
                >
                  編輯
                </Button>
                <Popconfirm
                  title="刪除這個專案？"
                  description="仍有任務時無法刪除，可以改用「取消」。"
                  okText="刪除"
                  cancelText="取消"
                  onConfirm={() => {
                    remove.mutate();
                  }}
                >
                  <Button danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Flex>
            ) : null}
          </Flex>
        </Flex>
      </Card>

      <div data-tour="project-tabs" data-tour-inner=".ant-tabs-nav">
        <Tabs
          activeKey={tab}
          onChange={(key) => {
            setSearchParams((current) => {
              const next = new URLSearchParams(current);
              next.set('tab', key);
              return next;
            }, { preventScrollReset: true });
          }}
          items={[
            {
              key: 'tasks',
              label: '任務',
              children: (
                <TasksTab
                  projectId={projectId}
                  wipLimits={project.wipLimits}
                  access={access}
                  directory={directory}
                  memberOptions={memberOptions}
                  onOpen={drawer.open}
                />
              ),
            },
            {
              key: 'members',
              label: `成員（${String(members.length)}）`,
              children: <MemberTab projectId={projectId} members={members} directory={directory} access={access} />,
            },
          ]}
        />
      </div>

      <ProjectFormModal
        open={editing}
        project={project}
        saving={update.isPending}
        onCancel={() => {
          setEditing(false);
        }}
        onSubmit={(values) => {
          update.mutate(values);
        }}
      />
      <TaskDrawer taskId={drawer.taskId} onClose={drawer.close} onOpen={drawer.open} />
      <PageTour id="project-detail" steps={PROJECT_DETAIL_TOUR} />
    </Flex>
  );
}
