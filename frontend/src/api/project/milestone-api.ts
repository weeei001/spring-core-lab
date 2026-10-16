import { createCrudApi } from '@/core/http/crud';
import type { Milestone, MilestoneQuery } from './milestone.types';

export const milestoneApi = createCrudApi<Milestone, MilestoneQuery>('/api/milestone');
