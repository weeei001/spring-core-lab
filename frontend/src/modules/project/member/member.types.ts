import type { BaseEntity } from '@/core/http/types';

export type ProjectRole = 'MANAGER' | 'MEMBER';

/** 專案成員（後端 ProjectMember）；每個專案至少一位經理 */
export interface ProjectMember extends BaseEntity {
  projectId: number;
  /** basic-data 的 user id */
  userId: string;
  role: ProjectRole;
}

export interface ProjectMemberQuery {
  ids?: number[];
  projectId?: number;
  projectIds?: number[];
  userId?: string;
  role?: ProjectRole;
}
