import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { TablePaginationConfig } from 'antd';
import type { PageQuery, PageResponse } from '@/core/http/types';

/**
 * 分頁列表：管理查詢條件與頁碼，回傳 antd Table 需要的 rows、loading、pagination。
 * queryKey 以 key 開頭，儲存或刪除後 invalidate 同一個 key 就會重新查詢。
 */
export function usePagedList<T, Q extends object>(
  key: string,
  fetchPage: (query: Q & PageQuery) => Promise<PageResponse<T>>,
  initialFilters: Q,
) {
  const [filters, setFilters] = useState<Q>(initialFilters);
  const [paging, setPaging] = useState({ page: 1, size: 20 });

  const list = useQuery({
    queryKey: [key, 'page', filters, paging],
    // 新的在前；後端只允許 id、createTime、transTime 排序
    queryFn: () => fetchPage({ ...filters, ...paging, sort: [{ field: 'id', direction: 'desc' }] }),
    // 換頁時保留上一頁的資料，表格不會閃一下變空白
    placeholderData: keepPreviousData,
  });

  const pagination: TablePaginationConfig = {
    current: paging.page,
    pageSize: paging.size,
    total: list.data?.total ?? 0,
    showSizeChanger: true,
    showTotal: (total) => `共 ${String(total)} 筆`,
    onChange: (page, size) => {
      setPaging({ page, size });
    },
  };

  /** 套用新的查詢條件並回到第一頁 */
  const search = (next: Q): void => {
    setFilters(next);
    setPaging((current) => ({ ...current, page: 1 }));
  };

  const rows: T[] = list.data?.items ?? [];
  return { rows, loading: list.isFetching, pagination, search };
}
