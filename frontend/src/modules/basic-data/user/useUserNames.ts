import { useQuery } from '@tanstack/react-query';
import type { BaseEntity } from '@/core/http/types';
import { userApi } from './user-api';

const SYSTEM = 'system';

/**
 * 把表格資料的 transId（最後修改者的 user id）換成名字：一次查回這一頁用到的使用者。
 * 一頁最多 100 筆，剛好在後端 ids 的上限內。
 */
export function useUserNames(rows: readonly BaseEntity[]): (id: string) => string {
  const ids = [...new Set(rows.map((row) => row.transId).filter((id) => id !== SYSTEM))].sort((a, b) =>
    a.localeCompare(b),
  );
  const { data } = useQuery({
    queryKey: ['user', 'names', ids],
    queryFn: () => userApi.query({ ids }),
    enabled: ids.length > 0,
    staleTime: 5 * 60 * 1000,
  });
  const names = new Map((data ?? []).map((user) => [String(user.id), user.name] as const));
  return (id) => (id === SYSTEM ? '系統' : (names.get(id) ?? id));
}
