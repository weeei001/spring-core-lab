import { useQuery } from '@tanstack/react-query';
import { userApi } from '@/api/basic-data/user-api';
import type { User } from '@/api/basic-data/user.types';

const SYSTEM = 'system';

export interface UserDirectory {
  /** 查不到（還在載入或已不存在）時回 undefined */
  user: (id: string) => User | undefined;
  /** 查不到時回 id；system 回「系統」 */
  name: (id: string) => string;
}

/**
 * 依 user id 一次查回多位使用者（成員、指派人、留言者等），5 分鐘內不重查；最多 100 位。
 * 和 useUserNames 不同的是可以拿到整筆 User（頭像要用 Email）。
 */
export function useUserDirectory(ids: readonly (string | null | undefined)[]): UserDirectory {
  const unique = [
    ...new Set(ids.filter((id): id is string => id !== null && id !== undefined && id !== SYSTEM)),
  ]
    .sort((a, b) => a.localeCompare(b))
    .slice(0, 100);
  const { data } = useQuery({
    queryKey: ['user', 'directory', unique],
    queryFn: () => userApi.query({ ids: unique }),
    enabled: unique.length > 0,
    staleTime: 5 * 60 * 1000,
  });
  const users = new Map((data ?? []).map((user) => [String(user.id), user] as const));
  return {
    user: (id) => users.get(id),
    name: (id) => (id === SYSTEM ? '系統' : (users.get(id)?.name ?? id)),
  };
}
