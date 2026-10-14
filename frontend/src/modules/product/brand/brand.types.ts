import type { BaseEntity } from '@/core/http/types';

/** 品牌（後端 Brand）；商品以 code 關聯 */
export interface Brand extends BaseEntity {
  /** 例如 APPLE；建立後不可修改、刪除後不可重用 */
  code: string;
  name: string;
}

export interface BrandQuery {
  keyword?: string;
  codes?: string[];
}
