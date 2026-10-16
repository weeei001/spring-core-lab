import { useEffect, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, Empty, Flex, Input, Modal, Spin, Tag, Typography, theme } from 'antd';
import { CheckSquareOutlined, ProjectOutlined, SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { projectApi } from '@/api/project/project-api';
import { taskApi } from '@/api/project/task-api';
import { ProjectStatusTag, TaskStatusTag } from '@/components/project/Tags';
import { DEFAULT_PROJECT_COLOR } from '@/components/project/labels';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { useDebounced } from '@/shared/hooks/useDebounced';

interface Item {
  key: string;
  icon: ReactNode;
  title: string;
  extra: ReactNode;
  path: string;
}

/**
 * 全站搜尋（Ctrl / ⌘ + K）：搜尋我參與的專案與指派給我的任務，上下鍵選擇、Enter 開啟。
 * 放在版面的上方列；沒有 project:read 時不顯示。
 */
export function GlobalSearch() {
  const { user, can } = useAuth();
  const navigate = useNavigate();
  const { token } = theme.useToken();
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [active, setActive] = useState(0);
  const trimmed = keyword.trim();
  const query = useDebounced(trimmed);
  // 重新打開時 query 還會留著上次的字 300ms，以目前輸入框為準
  const enabled = open && trimmed !== '' && query !== '';
  const myId = String(user.id);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent): void => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setOpen(true);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
    };
  }, []);

  const projects = useQuery({
    queryKey: ['project', 'search', myId, query],
    queryFn: () => projectApi.query({ keyword: query, memberUserId: myId }),
    enabled,
  });
  const tasks = useQuery({
    queryKey: ['task', 'search', myId, query],
    queryFn: () => taskApi.query({ keyword: query, assigneeId: myId }),
    enabled,
  });

  if (!can(PERMISSIONS.PROJECT_READ)) {
    return null;
  }

  const items: Item[] = [
    ...(projects.data ?? []).slice(0, 5).map((project) => ({
      key: `project-${String(project.id)}`,
      icon: <ProjectOutlined style={{ color: project.color ?? DEFAULT_PROJECT_COLOR }} />,
      title: project.name,
      extra: <ProjectStatusTag status={project.status} />,
      path: `/projects/${String(project.id)}`,
    })),
    ...(tasks.data ?? []).slice(0, 8).map((task) => ({
      key: `task-${String(task.id)}`,
      icon: <CheckSquareOutlined />,
      title: `${task.code} ${task.title}`,
      extra: <TaskStatusTag status={task.status} />,
      path: `/projects/${String(task.projectId)}?tab=tasks&task=${String(task.id)}`,
    })),
  ];
  const close = (): void => {
    setOpen(false);
    setKeyword('');
    setActive(0);
  };
  const go = (item: Item | undefined): void => {
    if (item !== undefined) {
      close();
      void navigate(item.path);
    }
  };
  const loading = enabled && (projects.isFetching || tasks.isFetching);

  return (
    <>
      <Button
        icon={<SearchOutlined />}
        onClick={() => {
          setOpen(true);
        }}
        style={{ color: token.colorTextSecondary }}
      >
        搜尋 <Typography.Text keyboard>Ctrl K</Typography.Text>
      </Button>
      <Modal open={open} onCancel={close} footer={null} closable={false} destroyOnHidden styles={{ body: { padding: 0 } }}>
        <Flex vertical>
          <Input
            autoFocus
            size="large"
            variant="borderless"
            prefix={<SearchOutlined />}
            suffix={loading ? <Spin size="small" /> : null}
            placeholder="搜尋專案、指派給我的任務"
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setActive(0);
            }}
            onKeyDown={(event) => {
              // 注音選字時按 Enter 是確認文字，不是開啟結果
              if (event.nativeEvent.isComposing) {
                return;
              }
              if (event.key === 'ArrowDown') {
                event.preventDefault();
                setActive((current) => Math.max(0, Math.min(current + 1, items.length - 1)));
              } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                setActive((current) => Math.max(current - 1, 0));
              } else if (event.key === 'Enter') {
                go(items[active]);
              }
            }}
            style={{ padding: '12px 16px', borderBottom: `1px solid ${token.colorBorderSecondary}` }}
          />
          <div style={{ maxHeight: 420, overflowY: 'auto', padding: 8 }}>
            {trimmed === '' ? (
              <Typography.Text type="secondary" style={{ display: 'block', padding: 16, textAlign: 'center' }}>
                輸入專案名稱、任務標題或編號
              </Typography.Text>
            ) : items.length === 0 && !loading ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="找不到符合的專案或任務" />
            ) : (
              items.map((item, index) => (
                <Flex
                  key={item.key}
                  align="center"
                  gap={10}
                  onMouseEnter={() => {
                    setActive(index);
                  }}
                  onClick={() => {
                    go(item);
                  }}
                  style={{
                    padding: '8px 12px',
                    borderRadius: token.borderRadius,
                    cursor: 'pointer',
                    background: index === active ? token.colorPrimaryBg : undefined,
                  }}
                >
                  {item.icon}
                  <Typography.Text ellipsis style={{ flex: 1 }}>
                    {item.title}
                  </Typography.Text>
                  {item.extra}
                  {index === active ? <Tag>Enter</Tag> : null}
                </Flex>
              ))
            )}
          </div>
        </Flex>
      </Modal>
    </>
  );
}
