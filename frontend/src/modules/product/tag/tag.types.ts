import type { BaseEntity } from '@/core/http/types';

/** 商品標籤（後端 Tag）；商品以 id 關聯（多對多） */
export interface Tag extends BaseEntity {
  name: string;
}

export interface TagQuery {
  keyword?: string;
  ids?: number[];
}
