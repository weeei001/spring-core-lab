import { Tooltip, Typography } from 'antd';
import { CalendarOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { dueText, isOverdue, isTaskFinished } from '../project-utils';
import type { Task } from '../task/task.types';

/** 到期日：逾期紅字、兩天內橘字，滑過顯示相對時間 */
export function DueDate({ task }: { task: Pick<Task, 'dueDate' | 'status'> }) {
  if (task.dueDate === null) {
    return <Typography.Text type="secondary">—</Typography.Text>;
  }
  const overdue = isOverdue(task);
  const soon = !overdue && !isTaskFinished(task.status) && dayjs(task.dueDate).diff(dayjs().startOf('day'), 'day') <= 2;
  return (
    <Tooltip title={dueText(task.dueDate)}>
      <Typography.Text type={overdue ? 'danger' : soon ? 'warning' : undefined} style={{ whiteSpace: 'nowrap' }}>
        <CalendarOutlined /> {dayjs(task.dueDate).format('MM/DD')}
      </Typography.Text>
    </Tooltip>
  );
}
