import { http, send } from '@/core/http/client';
import { createCrudApi } from '@/core/http/crud';
import type { Result } from '@/core/http/types';
import type { Product, ProductQuery } from './product.types';

export const productApi = {
  ...createCrudApi<Product, ProductQuery>('/api/product'),
  /** quantity 正數為入庫、負數為出庫；庫存不足時後端回 400 */
  adjustStock: (id: number, quantity: number) =>
    send(http.post<Result<Product>>(`/api/product/${String(id)}/stock`, undefined, { params: { quantity } })),
};
