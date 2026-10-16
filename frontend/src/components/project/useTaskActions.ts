import { useMutation } from '@tanstack/react-query';
import { App } from 'antd';
import { taskApi } from '@/api/project/task-api';
import type { Task, TaskStatus } from '@/api/project/task.types';
import { useInvalidateProjectData } from './hooks';
import { isTaskFinished } from './project-utils';

interface UpdateInput {
  id: number;
  values: Partial<Task>;
}

interface MoveInput {
  id: number;
  status: TaskStatus;
  previousId?: number;
}

/** 任務的新增、修改、刪除、拖曳；成功或失敗後都重新查詢，失敗訊息由 app/query-client 統一顯示 */
export function useTaskActions() {
  const invalidate = useInvalidateProjectData();
  const { message, modal } = App.useApp();

  const create = useMutation({
    mutationFn: (values: Partial<Task>) => taskApi.create(values),
    onSuccess: () => {
      void message.success('已建立任務');
    },
    onSettled: invalidate,
  });
  const update = useMutation({
    mutationFn: ({ id, values }: UpdateInput) => taskApi.update(id, values),
    onSettled: invalidate,
  });
  const remove = useMutation({
    mutationFn: (id: number) => taskApi.remove(id),
    onSuccess: () => {
      void message.success('已刪除任務');
    },
    onSettled: invalidate,
  });
  const move = useMutation({
    mutationFn: ({ id, status, previousId }: MoveInput) => taskApi.move(id, status, previousId),
    onSettled: invalidate,
  });

  /**
   * 改成進行中時，若前置任務還沒完成就先確認（PRD：警告但不擋）；回傳是否繼續。
   * allTasks 是同專案的任務，用來找前置任務的狀態。
   */
  const confirmStatus = (task: Task, status: TaskStatus, allTasks: readonly Task[]): Promise<boolean> => {
    if (status !== 'IN_PROGRESS' || task.predecessorIds.length === 0) {
      return Promise.resolve(true);
    }
    const pending = allTasks.filter((other) => task.predecessorIds.includes(other.id) && !isTaskFinished(other.status));
    if (pending.length === 0) {
      return Promise.resolve(true);
    }
    return new Promise((resolve) => {
      modal.confirm({
        title: '前置任務還沒完成',
        content: `${pending.map((other) => `${other.code} ${other.title}`).join('、')} 尚未完成，確定要開始這個任務？`,
        okText: '仍要開始',
        cancelText: '取消',
        onOk: () => {
          resolve(true);
        },
        onCancel: () => {
          resolve(false);
        },
      });
    });
  };

  return { create, update, remove, move, confirmStatus };
}
