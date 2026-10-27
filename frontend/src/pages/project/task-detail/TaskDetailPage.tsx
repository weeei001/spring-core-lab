import { ArrowLeftOutlined } from '@ant-design/icons';
import { Button, Card, Flex, Result } from 'antd';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { TaskDetailContent } from '@/components/project/task-drawer/TaskDetailContent';
import { taskDetailPath, taskSourcePath } from '@/components/project/task-drawer/navigation';
import './task-detail-page.css';

export function TaskDetailPage() {
  const { taskId: value } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const taskId = Number(value);
  const from = taskSourcePath(params.get('from'));

  if (!Number.isSafeInteger(taskId) || taskId <= 0) {
    return <Result status="404" title="找不到任務" extra={<Link to={from ?? '/projects'}>返回</Link>} />;
  }

  return (
    <Card className="task-detail-page">
      <TaskDetailContent
        key={taskId}
        taskId={taskId}
        navigation={(projectId) => (
          <Flex align="center" justify="space-between" gap={12} wrap>
            <Link to={from ?? (projectId === undefined ? '/projects' : `/projects/${String(projectId)}`)}>
              <Button icon={<ArrowLeftOutlined />}>{from === undefined ? '返回專案' : '返回來源'}</Button>
            </Link>
            {projectId === undefined ? null : <Link to={`/projects/${String(projectId)}`}>所屬專案</Link>}
          </Flex>
        )}
        onOpen={(id) => { void navigate(taskDetailPath(id, from)); }}
        onDeleted={(projectId) => { void navigate(from ?? `/projects/${String(projectId)}`); }}
      />
    </Card>
  );
}
