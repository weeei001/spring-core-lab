import { createCrudApi } from '@/core/http/crud';
import type { Tag, TagQuery } from './tag.types';

export const tagApi = createCrudApi<Tag, TagQuery>('/api/tag');
