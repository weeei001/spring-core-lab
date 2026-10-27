import { useMemo, useState } from 'react';
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  type UniqueIdentifier,
} from '@dnd-kit/core';
import { SortableContext, arrayMove, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Badge, Flex, Input, Tooltip, Typography, theme } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { Task, TaskStatus } from '@/api/project/task.types';
import { TASK_STATUSES, TASK_STATUS_COLORS, TASK_STATUS_LABELS } from '@/components/project/labels';
import { useTaskActions } from '@/components/project/useTaskActions';
import type { UserDirectory } from '@/components/user/useUserDirectory';
import { TaskCard } from './TaskCard';

type Columns = Record<TaskStatus, number[]>;

function emptyColumns(): Columns {
  return { TODO: [], IN_PROGRESS: [], REVIEW: [], DONE: [], CANCELLED: [] };
}

function cloneColumns(columns: Columns): Columns {
  const next = emptyColumns();
  for (const status of TASK_STATUSES) {
    next[status] = [...columns[status]];
  }
  return next;
}

function isStatus(id: UniqueIdentifier): id is TaskStatus {
  return typeof id === 'string' && (TASK_STATUSES as readonly string[]).includes(id);
}

/** id 是欄位（狀態）就回該欄；是任務就回它目前所在的欄 */
function columnOf(id: UniqueIdentifier, columns: Columns): TaskStatus | undefined {
  if (isStatus(id)) {
    return id;
  }
  return TASK_STATUSES.find((status) => columns[status].includes(Number(id)));
}

interface SortableTaskProps {
  task: Task;
  parent: Task | undefined;
  directory: UserDirectory;
  disabled: boolean;
  onOpen: (taskId: number) => void;
}

function SortableTask({ task, parent, directory, disabled, onOpen }: SortableTaskProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id, disabled });
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.35 : 1 }}
      {...attributes}
      {...listeners}
    >
      <TaskCard task={task} parent={parent} directory={directory} onOpen={onOpen} />
    </div>
  );
}

interface BoardColumnProps {
  status: TaskStatus;
  tasks: Task[];
  tasksById: ReadonlyMap<number, Task>;
  /** 這一欄實際的主任務數（不受篩選影響），和 WIP 上限比較 */
  total: number;
  wipLimit: number | undefined;
  directory: UserDirectory;
  editable: boolean;
  onOpen: (taskId: number) => void;
  onQuickAdd: (status: TaskStatus, title: string) => void;
}

function BoardColumn({ status, tasks, tasksById, total, wipLimit, directory, editable, onOpen, onQuickAdd }: BoardColumnProps) {
  const { token } = theme.useToken();
  const { setNodeRef, isOver } = useDroppable({ id: status });
  const [title, setTitle] = useState('');
  const over = wipLimit !== undefined && total > wipLimit;
  const full = wipLimit !== undefined && total === wipLimit;
  return (
    <Flex
      vertical
      gap={8}
      style={{
        flex: '1 0 260px',
        maxWidth: 340,
        padding: 10,
        borderRadius: token.borderRadiusLG,
        background: isOver ? token.colorPrimaryBg : over ? token.colorErrorBg : token.colorFillQuaternary,
        border: `1px solid ${over ? token.colorErrorBorder : 'transparent'}`,
        transition: 'background 0.2s',
      }}
    >
      <Flex align="center" justify="space-between" style={{ padding: '2px 4px' }}>
        <Badge color={TASK_STATUS_COLORS[status]} text={<Typography.Text strong>{TASK_STATUS_LABELS[status]}</Typography.Text>} />
        {wipLimit === undefined ? (
          <Typography.Text type="secondary">{String(tasks.length)}</Typography.Text>
        ) : (
          <Tooltip
            title={
              over
                ? `超過 WIP 上限 ${String(total - wipLimit)} 張，先把手上的做完再拉新的`
                : `主任務 WIP 上限 ${String(wipLimit)} 張${full ? '，已滿' : ''}`
            }
          >
            <Typography.Text type={over ? 'danger' : full ? 'warning' : 'secondary'} strong={over}>
              {String(total)} / {String(wipLimit)}
            </Typography.Text>
          </Tooltip>
        )}
      </Flex>
      <SortableContext items={tasks.map((task) => task.id)} strategy={verticalListSortingStrategy}>
        <div ref={setNodeRef} style={{ display: 'flex', flexDirection: 'column', gap: 8, minHeight: 80 }}>
          {tasks.map((task) => (
            <SortableTask
              key={task.id}
              task={task}
              parent={task.parentId === null ? undefined : tasksById.get(task.parentId)}
              directory={directory}
              disabled={!editable}
              onOpen={onOpen}
            />
          ))}
        </div>
      </SortableContext>
      {editable ? (
        <Input
          variant="borderless"
          prefix={<PlusOutlined />}
          placeholder="新增任務，按 Enter"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
          }}
          onPressEnter={() => {
            const trimmed = title.trim();
            if (trimmed !== '') {
              onQuickAdd(status, trimmed);
              setTitle('');
            }
          }}
        />
      ) : null}
    </Flex>
  );
}

interface TaskBoardViewProps {
  /** 與表格共用的篩選結果，含子任務。 */
  tasks: readonly Task[];
  allTasks: readonly Task[];
  directory: UserDirectory;
  editable: boolean;
  showCancelled: boolean;
  /** 專案的 WIP 上限 */
  wipLimits: Partial<Record<TaskStatus, number>>;
  onOpen: (taskId: number) => void;
  onQuickAdd: (status: TaskStatus, title: string) => void;
}

/**
 * 看板：依狀態分欄，拖曳卡片改狀態與順序。拖曳中先改畫面（dragColumns），放開後呼叫 move，
 * 重新查詢完成才清掉暫存；失敗時畫面回到伺服器的資料。
 */
export function TaskBoardView({ tasks, allTasks, directory, editable, showCancelled, wipLimits, onOpen, onQuickAdd }: TaskBoardViewProps) {
  const { move, confirmStatus } = useTaskActions();
  const sensors = useSensors(
    // 移動 5px 才算拖曳，單純點擊會打開任務
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );
  const [activeId, setActiveId] = useState<number | null>(null);
  const [dragColumns, setDragColumns] = useState<Columns | null>(null);

  const tasksById = useMemo(() => new Map(allTasks.map((task) => [task.id, task] as const)), [allTasks]);
  const serverColumns = useMemo(() => {
    const columns = emptyColumns();
    const sorted = [...tasks].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);
    for (const task of sorted) {
      columns[task.status].push(task.id);
    }
    return columns;
  }, [tasks]);
  const columns = dragColumns ?? serverColumns;
  // WIP 看的是整個專案的主任務，不受篩選影響
  const totals = useMemo(() => {
    const counts = new Map<TaskStatus, number>();
    for (const task of allTasks) {
      if (task.parentId === null) {
        counts.set(task.status, (counts.get(task.status) ?? 0) + 1);
      }
    }
    return counts;
  }, [allTasks]);
  const statuses = showCancelled ? TASK_STATUSES : TASK_STATUSES.filter((status) => status !== 'CANCELLED');

  const handleDragStart = ({ active }: DragStartEvent): void => {
    setActiveId(Number(active.id));
    setDragColumns(cloneColumns(serverColumns));
  };

  /** 拖到別的欄位時，先把卡片搬過去，該欄才會讓出位置 */
  const handleDragOver = ({ active, over }: DragOverEvent): void => {
    if (over === null) {
      return;
    }
    setDragColumns((current) => {
      const base = current ?? cloneColumns(serverColumns);
      const from = columnOf(active.id, base);
      const to = columnOf(over.id, base);
      if (from === undefined || to === undefined || from === to) {
        return base;
      }
      const next = cloneColumns(base);
      next[from] = next[from].filter((id) => id !== Number(active.id));
      const overIndex = isStatus(over.id) ? -1 : next[to].indexOf(Number(over.id));
      const index = overIndex < 0 ? next[to].length : overIndex;
      next[to].splice(index, 0, Number(active.id));
      return next;
    });
  };

  const handleDragEnd = async ({ active, over }: DragEndEvent): Promise<void> => {
    setActiveId(null);
    const id = Number(active.id);
    const task = tasksById.get(id);
    const current = dragColumns;
    if (over === null || task === undefined || current === null) {
      setDragColumns(null);
      return;
    }
    const status = columnOf(id, current);
    if (status === undefined) {
      setDragColumns(null);
      return;
    }
    // 同一欄內換位置
    let list = current[status];
    if (!isStatus(over.id) && list.includes(Number(over.id))) {
      list = arrayMove(list, list.indexOf(id), list.indexOf(Number(over.id)));
    }
    const final = cloneColumns(current);
    final[status] = list;
    setDragColumns(final);

    const index = list.indexOf(id);
    const previousId = index > 0 ? list[index - 1] : undefined;
    const original = serverColumns[task.status];
    const originalIndex = original.indexOf(id);
    const originalPrevious = originalIndex > 0 ? original[originalIndex - 1] : undefined;
    if (status === task.status && previousId === originalPrevious) {
      setDragColumns(null);
      return;
    }
    if (!(await confirmStatus(task, status, allTasks))) {
      setDragColumns(null);
      return;
    }
    move.mutate(
      { id, status, previousId },
      {
        onSettled: () => {
          setDragColumns(null);
        },
      },
    );
  };

  const activeTask = activeId === null ? undefined : tasksById.get(activeId);

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={(event) => {
        void handleDragEnd(event);
      }}
      onDragCancel={() => {
        setActiveId(null);
        setDragColumns(null);
      }}
    >
      <Flex gap={12} align="flex-start" style={{ overflowX: 'auto', paddingBottom: 8 }}>
        {statuses.map((status) => (
          <BoardColumn
            key={status}
            status={status}
            tasks={columns[status].map((id) => tasksById.get(id)).filter((task): task is Task => task !== undefined)}
            tasksById={tasksById}
            total={totals.get(status) ?? 0}
            wipLimit={wipLimits[status]}
            directory={directory}
            editable={editable}
            onOpen={onOpen}
            onQuickAdd={onQuickAdd}
          />
        ))}
      </Flex>
      <DragOverlay>
        {activeTask === undefined ? null : (
          <TaskCard
            task={activeTask}
            parent={activeTask.parentId === null ? undefined : tasksById.get(activeTask.parentId)}
            directory={directory}
            overlay
          />
        )}
      </DragOverlay>
    </DndContext>
  );
}
