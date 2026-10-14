import { createCrudApi } from '@/core/http/crud';
import type { Brand, BrandQuery } from './brand.types';

export const brandApi = createCrudApi<Brand, BrandQuery>('/api/brand');
