import { useMutation, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import type { CrudApi } from '@/core/http/crud';

interface SaveInput<T> {
  /** 有 id 為修改，沒有為新增 */
  id?: number;
  values: Partial<T>;
}

/** 新增／修改／刪除；成功後重新查詢 key 底下的資料。失敗訊息由 app/query-client 統一顯示 */
export function useSaveDelete<T>(key: string, resource: Pick<CrudApi<T, never>, 'create' | 'update' | 'remove'>) {
  const queryClient = useQueryClient();
  const refresh = () => queryClient.invalidateQueries({ queryKey: [key] });

  const save = useMutation({
    mutationFn: ({ id, values }: SaveInput<T>) => (id === undefined ? resource.create(values) : resource.update(id, values)),
    onSuccess: () => {
      void message.success('已儲存');
      return refresh();
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => resource.remove(id),
    onSuccess: () => {
      void message.success('已刪除');
      return refresh();
    },
  });

  return { save, remove };
}
