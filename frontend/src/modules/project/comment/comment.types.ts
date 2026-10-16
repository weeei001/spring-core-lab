import type { BaseEntity } from '@/core/http/types';

/** 任務留言（後端 TaskComment）；作者是 createId */
export interface TaskComment extends BaseEntity {
  taskId: number;
  content: string;
}

export interface TaskCommentQuery {
  taskId?: number;
  ids?: number[];
}
