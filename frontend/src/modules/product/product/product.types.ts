import type { BaseEntity } from '@/core/http/types';
import type { Brand } from '../brand/brand.types';

export type ProductCategory = 'ELECTRONICS' | 'BOOK' | 'FOOD' | 'CLOTHING' | 'OTHER';

/** 商品（後端 Product） */
export interface Product extends BaseEntity {
  /** 系統編號，新增時由 basic-data 取號，不可修改 */
  code: string;
  /** 貨號，使用者自訂，不可重複 */
  sku: string;
  name: string;
  category: ProductCategory;
  price: number;
  /** 只能用調整庫存修改 */
  stock: number;
  onSale: boolean;
  /** 多對一：品牌代碼；修改品牌請送這個欄位 */
  brandCode: string | null;
  /** 後端帶回的品牌資料，只用來顯示 */
  brand: Brand | null;
  /** 多對多：標籤 id；給清單整批取代，給 [] 清空 */
  tagIds: number[];
}

export interface ProductQuery {
  /** 編號、貨號或名稱包含的文字；不可為空字串 */
  keyword?: string;
  categories?: ProductCategory[];
  minPrice?: number;
  maxPrice?: number;
  onSale?: boolean;
  brandCode?: string;
  tagId?: number;
}
