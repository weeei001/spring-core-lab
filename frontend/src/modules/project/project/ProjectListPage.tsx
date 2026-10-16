import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Col, Empty, Flex, Input, Pagination, Progress, Row, Segmented, Select, Skeleton, Switch, Table, Typography, type TableColumnsType } from 'antd';
import { AppstoreOutlined, PlusOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { useAuth } from '@/core/auth/useAuth';
import { PERMISSIONS } from '@/core/auth/permissions';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { PageTour } from '@/shared/ui/PageTour';
import { blankToUndefined, toOptions } from '@/shared/lib/format';
import { PROJECT_LIST_TOUR } from '../guide/tours';
import { useInvalidateProjectData } from '../hooks';
import { PROJECT_ROLE_LABELS, PROJECT_STATUS_LABELS } from '../labels';
import { memberApi } from '../member/member-api';
import { percent } from '../project-utils';
import { ProjectStatusTag } from '../ui/Tags';
import { ProjectCard } from './ProjectCard';
import { ProjectFormModal } from './ProjectFormModal';
import { projectApi } from './project-api';
import type { Project, ProjectQuery, ProjectStatus } from './project.types';

type ViewMode = 'card' | 'table';

const VIEW_KEY = 'project-list-view';
const OPEN_STATUSES: ProjectStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD'];

/** 記住上次選的檢視；瀏覽器不允許存取 localStorage 時用預設值 */
function loadView(): ViewMode {
  try {
    return localStorage.getItem(VIEW_KEY) === 'table' ? 'table' : 'card';
  } catch {
    return 'card';
  }
}

function saveView(view: ViewMode): void {
  try {
    localStorage.setItem(VIEW_KEY, view);
  } catch {
    // 存不了就算了，下次用預設值
  }
}

/** 專案列表：卡片或表格，可依關鍵字、狀態、我是經理篩選；沒有 project:admin 只看得到自己參與的 */
export function ProjectListPage() {
  const { user, can } = useAuth();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const invalidate = useInvalidateProjectData();
  const myId = String(user.id);
  const [view, setView] = useState<ViewMode>(loadView);
  const [filters, setFilters] = useState<ProjectQuery>({ statuses: OPEN_STATUSES });
  const [creating, setCreating] = useState(false);
  const list = usePagedList<Project, ProjectQuery>('project', projectApi.queryPage, filters);

  // 我在這一頁各專案的角色（沒有 project:admin 時後端只回自己的成員資料）
  const projectIds = list.rows.map((project) => project.id);
  const myRoles = useQuery({
    queryKey: ['project-member', 'mine', projectIds],
    queryFn: () => memberApi.query({ projectIds, userId: myId }),
    enabled: projectIds.length > 0,
  });
  const roleOf = (projectId: number) => myRoles.data?.find((member) => member.projectId === projectId)?.role;

  const create = useMutation({
    mutationFn: (values: Partial<Project>) => projectApi.create(values),
    onSuccess: (project) => {
      void message.success('已建立專案');
      setCreating(false);
      void navigate(`/projects/${String(project.id)}`);
    },
    onSettled: invalidate,
  });

  const apply = (next: ProjectQuery): void => {
    setFilters(next);
    list.search(next);
  };
  const open = (projectId: number): void => {
    void navigate(`/projects/${String(projectId)}`);
  };

  const columns: TableColumnsType<Project> = [
    { title: '編號', dataIndex: 'code', width: 120 },
    {
      title: '名稱',
      key: 'name',
      render: (_value, project) => (
        <Flex align="center" gap={8}>
          <span style={{ width: 10, height: 10, borderRadius: '50%', background: project.color ?? '#1677FF', flexShrink: 0 }} />
          <Typography.Link
            onClick={() => {
              open(project.id);
            }}
          >
            {project.name}
          </Typography.Link>
        </Flex>
      ),
    },
    { title: '狀態', key: 'status', width: 100, render: (_value, project) => <ProjectStatusTag status={project.status} /> },
    {
      title: '進度',
      key: 'progress',
      width: 200,
      render: (_value, project) => (
        <Progress percent={percent(project.doneTaskCount, project.taskCount)} size="small" strokeColor={project.color ?? undefined} />
      ),
    },
    {
      title: '任務',
      key: 'tasks',
      width: 100,
      render: (_value, project) => `${String(project.doneTaskCount ?? 0)} / ${String(project.taskCount ?? 0)}`,
    },
    {
      title: '逾期',
      key: 'overdue',
      width: 80,
      render: (_value, project) =>
        (project.overdueTaskCount ?? 0) > 0 ? (
          <Typography.Text type="danger">{String(project.overdueTaskCount)}</Typography.Text>
        ) : (
          <Typography.Text type="secondary">0</Typography.Text>
        ),
    },
    { title: '開始', dataIndex: 'startDate', width: 110 },
    { title: '結束', dataIndex: 'endDate', width: 110 },
    {
      title: '我的角色',
      key: 'role',
      width: 90,
      render: (_value, project) => {
        const role = roleOf(project.id);
        return role === undefined ? null : PROJECT_ROLE_LABELS[role];
      },
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          專案
        </Typography.Title>
        {can(PERMISSIONS.PROJECT_WRITE) ? (
          <Button
            type="primary"
            data-tour="project-create"
            icon={<PlusOutlined />}
            onClick={() => {
              setCreating(true);
            }}
          >
            新增專案
          </Button>
        ) : null}
      </Flex>

      <Flex justify="space-between" align="center" wrap gap={12}>
        <Flex gap={12} wrap align="center" data-tour="project-filters">
          <Input.Search
            placeholder="編號或名稱"
            allowClear
            style={{ width: 220 }}
            onSearch={(value) => {
              apply({ ...filters, keyword: blankToUndefined(value) });
            }}
          />
          <Select<ProjectStatus[]>
            mode="multiple"
            allowClear
            placeholder="全部狀態"
            value={filters.statuses ?? []}
            options={toOptions(PROJECT_STATUS_LABELS)}
            style={{ minWidth: 240 }}
            maxTagCount="responsive"
            onChange={(statuses) => {
              apply({ ...filters, statuses: statuses.length === 0 ? undefined : statuses });
            }}
          />
          <Flex gap={6} align="center">
            <Switch
              size="small"
              checked={filters.memberRole === 'MANAGER'}
              onChange={(checked) => {
                apply({ ...filters, memberUserId: checked ? myId : undefined, memberRole: checked ? 'MANAGER' : undefined });
              }}
            />
            <Typography.Text>我是經理</Typography.Text>
          </Flex>
        </Flex>
        <div data-tour="project-view">
          <Segmented<ViewMode>
            value={view}
            onChange={(next) => {
              setView(next);
              saveView(next);
            }}
            options={[
              { value: 'card', icon: <AppstoreOutlined />, label: '卡片' },
              { value: 'table', icon: <UnorderedListOutlined />, label: '表格' },
            ]}
          />
        </div>
      </Flex>

      <div data-tour="project-cards">
        {view === 'table' ? (
          <Table<Project> rowKey="id" columns={columns} dataSource={list.rows} loading={list.loading} pagination={list.pagination} />
        ) : list.loading && list.rows.length === 0 ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : list.rows.length === 0 ? (
          <Empty description="沒有符合條件的專案" />
        ) : (
          <Flex vertical gap={16}>
            <Row gutter={[16, 16]}>
              {list.rows.map((project, index) => (
                <Col key={project.id} xs={24} sm={12} xl={8} xxl={6}>
                  <ProjectCard project={project} myRole={roleOf(project.id)} index={index} onOpen={open} />
                </Col>
              ))}
            </Row>
            <Flex justify="end">
              <Pagination {...list.pagination} />
            </Flex>
          </Flex>
        )}
      </div>

      <ProjectFormModal
        open={creating}
        saving={create.isPending}
        onCancel={() => {
          setCreating(false);
        }}
        onSubmit={(values) => {
          create.mutate(values);
        }}
      />
      <PageTour id="project-list" steps={PROJECT_LIST_TOUR} />
    </Flex>
  );
}
