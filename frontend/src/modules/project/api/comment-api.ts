import { createCrudApi } from '@/core/http/crud';
import type { TaskComment, TaskCommentQuery } from './comment.types';

export const commentApi = createCrudApi<TaskComment, TaskCommentQuery>('/api/task-comment');
