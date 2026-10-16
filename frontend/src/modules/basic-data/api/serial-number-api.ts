import { createCrudApi } from '@/core/http/crud';
import type { SerialNumber, SerialNumberQuery } from './serial-number.types';

export const serialNumberApi = createCrudApi<SerialNumber, SerialNumberQuery>('/api/basic-data/serial-number');
