import { useEffect, useRef, useState } from 'react';
import Gantt, { type FrappeGanttTask, type FrappeGanttViewMode } from 'frappe-gantt';
import './gantt.css';
import dayjs from 'dayjs';
import { Card, Empty, Flex, Segmented, Typography, theme } from 'antd';
import type { Task } from '@/api/project/task.types';
import { TaskStatusTag } from '@/components/project/Tags';
import { DATE_FORMAT, isOverdue, percent } from '@/components/project/project-utils';

const VIEW_MODES: { value: FrappeGanttViewMode; label: string }[] = [
  { value: 'Day', label: '日' },
  { value: 'Week', label: '週' },
  { value: 'Month', label: '月' },
];

interface TaskGanttViewProps {
  /** 篩選後的任務（含子任務） */
  tasks: readonly Task[];
  /** 可拖曳改任務日期 */
  editable: boolean;
  onOpen: (taskId: number) => void;
  /** 儲存失敗時 reject，圖表會把長條畫回原位 */
  onReschedule: (task: Task, startDate: string, dueDate: string) => Promise<unknown>;
}

/**
 * frappe-gantt 用 innerHTML 顯示任務名稱（長條文字與說明框），名稱是使用者輸入的，
 * 不跳脫的話標題裡的 HTML 會被執行（XSS）。
 */
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** frappe-gantt 回來的結束時間可能是隔天 0 點或當天 23:59:59，退一秒後取日期，兩種都對 */
function endDate(end: Date): string {
  return dayjs(end).subtract(1, 'second').format(DATE_FORMAT);
}

function toGanttTask(task: Task, scheduledIds: Set<number>): FrappeGanttTask {
  // 只有一邊有日期時，當成一天的任務
  const start = task.startDate ?? task.dueDate ?? '';
  const end = task.dueDate ?? task.startDate ?? '';
  const hasSubtasks = task.subtaskCount !== undefined && task.subtaskCount > 0;
  return {
    id: String(task.id),
    name: escapeHtml(`${task.parentId === null ? '' : '↳ '}${task.code} ${task.title}`),
    start,
    end,
    progress: task.status === 'DONE' ? 100 : hasSubtasks ? percent(task.doneSubtaskCount, task.subtaskCount) : 0,
    dependencies: task.predecessorIds
      .filter((id) => scheduledIds.has(id))
      .map(String)
      .join(','),
    custom_class: isOverdue(task) ? 'gantt-overdue' : `gantt-status-${task.status}`,
  };
}

/**
 * 甘特圖（frappe-gantt）：長條是開始日到到期日，箭頭是前置任務；拖曳長條改日期。
 * 沒有日期的任務列在右側「未排程」。
 */
export function TaskGanttView({
  tasks,
  editable,
  onOpen,
  onReschedule,
}: TaskGanttViewProps) {
  const { token } = theme.useToken();
  const container = useRef<HTMLDivElement>(null);
  const [viewMode, setViewMode] = useState<FrappeGanttViewMode>('Week');
  // 拖曳沒被採用（沒權限或儲存失敗）時，frappe-gantt 的長條會停在放開的位置，資料沒變就不會重畫，所以另外計數強制重畫
  const [redraw, setRedraw] = useState(0);

  const scheduled = tasks.filter((task) => task.startDate !== null || task.dueDate !== null);
  const unscheduled = tasks.filter((task) => task.startDate === null && task.dueDate === null);
  const scheduledIds = new Set(scheduled.map((task) => task.id));
  const ganttTasks: FrappeGanttTask[] = scheduled
    .map((task) => toGanttTask(task, scheduledIds))
    .sort((a, b) => a.start.localeCompare(b.start));

  // 事件處理放在 ref，圖表不必因為 callback 換了就重畫
  const handlers = useRef({ tasks, onOpen, onReschedule });
  handlers.current = { tasks, onOpen, onReschedule };

  // 資料有變才重畫；用 JSON 字串比較內容，避免每次 render 都重建
  const signature = JSON.stringify(ganttTasks);
  const readonly = !editable;

  useEffect(() => {
    const node = container.current;
    if (node === null) {
      return undefined;
    }
    node.innerHTML = '';
    const data = JSON.parse(signature) as FrappeGanttTask[];
    if (data.length === 0) {
      return undefined;
    }
    new Gantt(node, data, {
      view_mode: viewMode,
      language: 'zh-TW',
      readonly,
      readonly_progress: true,
      bar_height: 24,
      padding: 16,
      today_button: true,
      view_mode_select: false,
      scroll_to: 'today',
      // 每個任務的日期各自儲存，不連帶移動後續任務
      move_dependencies: false,
      // 預設說明框是英文；name 已經跳脫過，日期是程式產生的
      popup: (context) => {
        context.set_title(context.task.name);
        context.set_subtitle('');
        context.set_details(
          `${dayjs(context.task._start).format('YYYY-MM-DD')} ～ ${endDate(context.task._end)}  進度 ${String(Math.round(context.task.progress ?? 0))}%`,
        );
      },
      on_click: (ganttTask) => {
        handlers.current.onOpen(Number(ganttTask.id));
      },
      on_date_change: (ganttTask, start, end) => {
        const current = handlers.current;
        const restore = (): void => {
          setRedraw((count) => count + 1);
        };
        const task = current.tasks.find((item) => String(item.id) === ganttTask.id);
        if (task !== undefined && editable) {
          current.onReschedule(task, dayjs(start).format(DATE_FORMAT), endDate(end)).catch(restore);
        } else {
          restore();
        }
      },
    });
    // 套件內建的「Today」按鈕是英文
    const todayButton = node.querySelector('.today-button');
    if (todayButton !== null) {
      todayButton.textContent = '今天';
    }
    return () => {
      node.innerHTML = '';
    };
  }, [signature, viewMode, readonly, editable, redraw]);

  return (
    <Flex gap={16} align="flex-start" wrap>
      <Flex vertical gap={12} style={{ flex: '1 1 600px', minWidth: 0 }}>
        <Flex justify="flex-end" align="center">
          <Segmented<FrappeGanttViewMode> options={VIEW_MODES} value={viewMode} onChange={setViewMode} />
        </Flex>
        {ganttTasks.length === 0 ? (
          <Empty description="還沒有排定日期的任務" />
        ) : (
          <div
            ref={container}
            className="project-gantt"
            style={{
              overflowX: 'auto',
              background: token.colorBgContainer,
              borderRadius: token.borderRadiusLG,
              border: `1px solid ${token.colorBorderSecondary}`,
            }}
          />
        )}
      </Flex>
      <Card size="small" title={`未排程（${String(unscheduled.length)}）`} style={{ flex: '0 0 260px' }}>
        {unscheduled.length === 0 ? (
          <Typography.Text type="secondary">所有任務都有日期</Typography.Text>
        ) : (
          <Flex vertical gap={8}>
            {unscheduled.map((task) => (
              <Flex key={task.id} justify="space-between" align="center" gap={8}>
                <Typography.Link
                  ellipsis
                  onClick={() => {
                    onOpen(task.id);
                  }}
                >
                  {task.title}
                </Typography.Link>
                <TaskStatusTag status={task.status} />
              </Flex>
            ))}
          </Flex>
        )}
      </Card>
    </Flex>
  );
}
