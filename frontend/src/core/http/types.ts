// 與後端共用的回應格式與基底型別

/** web-server 回給前端的統一格式（後端 Result） */
export interface Result<T> {
  result: boolean;
  data: T;
  errorCode: string | null;
  message: string | null;
}

/** 分頁查詢結果，page 從 1 起算（後端 PageResponse） */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface SortOrder {
  field: string;
  direction: 'asc' | 'desc';
}

/** 分頁、排序參數（後端 QueryParameter 的共用欄位） */
export interface PageQuery {
  page?: number;
  size?: number;
  sort?: SortOrder[];
}

/** 所有資料的共用欄位（後端 BaseEntity） */
export interface BaseEntity {
  id: number;
  /** 建立者的 user id；系統操作為 system */
  createId: string;
  /** LocalDateTime 字串，例如 2026-10-14T10:48:51.123456 */
  createTime: string;
  /** 最後修改者的 user id */
  transId: string;
  transTime: string;
}
