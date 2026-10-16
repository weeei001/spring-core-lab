import { createCrudApi } from '@/core/http/crud';
import type { ProjectLabel, ProjectLabelQuery } from './label.types';

export const labelApi = createCrudApi<ProjectLabel, ProjectLabelQuery>('/api/project-label');
