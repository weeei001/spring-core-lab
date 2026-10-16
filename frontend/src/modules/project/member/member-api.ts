import { createCrudApi } from '@/core/http/crud';
import type { ProjectMember, ProjectMemberQuery } from './member.types';

export const memberApi = createCrudApi<ProjectMember, ProjectMemberQuery>('/api/project-member');
