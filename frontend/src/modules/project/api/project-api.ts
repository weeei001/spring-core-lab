import { createCrudApi } from '@/core/http/crud';
import type { Project, ProjectQuery } from './project.types';

export const projectApi = createCrudApi<Project, ProjectQuery>('/api/project');
