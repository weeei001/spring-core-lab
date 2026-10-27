import { ExpandOutlined } from '@ant-design/icons';
import { Button, Drawer } from 'antd';
import { Link, useLocation } from 'react-router';
import { TaskDetailContent } from './TaskDetailContent';
import { taskDetailPath } from './navigation';

interface TaskDrawerProps {
  /** undefined 表示關閉 */
  taskId: number | undefined;
  onClose: () => void;
  onOpen: (taskId: number) => void;
}

/** 舊的 ?task= 入口繼續使用，共用完整頁面的任務內容。 */
export function TaskDrawer({ taskId, onClose, onOpen }: TaskDrawerProps) {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  params.delete('task');
  const search = params.toString();
  const from = `${location.pathname}${search === '' ? '' : `?${search}`}${location.hash}`;

  return (
    <Drawer
      open={taskId !== undefined}
      onClose={onClose}
      size={720}
      destroyOnHidden
      title="任務"
      styles={{ body: { padding: 16 } }}
      extra={taskId === undefined ? null : (
        <Link to={taskDetailPath(taskId, from)}>
          <Button icon={<ExpandOutlined />}>開啟完整頁面</Button>
        </Link>
      )}
    >
      {taskId === undefined ? null : (
        <TaskDetailContent key={taskId} taskId={taskId} onDeleted={onClose} onOpen={onOpen} />
      )}
    </Drawer>
  );
}
