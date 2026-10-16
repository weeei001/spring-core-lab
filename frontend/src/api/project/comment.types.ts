import type { BaseEntity } from '@/core/http/types';

/** 任務留言（後端 TaskComment）；作者是 createId */
export interface TaskComment extends BaseEntity {
  taskId: number;
  content: string;
  /** 留言中 @ 到的人（user id）；內文的「@名字」只是顯示用 */
  mentionIds: string[];
}

export interface TaskCommentQuery {
  taskId?: number;
  ids?: number[];
}
