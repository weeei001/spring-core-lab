import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import { labelApi } from '@/api/project/label-api';
import { memberApi } from '@/api/project/member-api';
import type { ProjectMember, ProjectRole } from '@/api/project/member.types';
import { milestoneApi } from '@/api/project/milestone-api';
import { projectApi } from '@/api/project/project-api';
import { sprintApi } from '@/api/project/sprint-api';
import type { Project } from '@/api/project/project.types';
import { taskApi } from '@/api/project/task-api';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { isProjectClosed } from './project-utils';

/** 專案相關資料的 queryKey 開頭；任何異動後一起重新查詢，畫面上的統計、進度才會一致 */
const PROJECT_KEYS = new Set(['project', 'project-member', 'milestone', 'project-label', 'sprint', 'task', 'task-comment', 'task-activity', 'task-attachment', 'work-log']);

export function useInvalidateProjectData(): () => Promise<void> {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ predicate: (query) => PROJECT_KEYS.has(String(query.queryKey[0])) });
}

/** 登入者在這個專案能做什麼；後端 web-server 也會再檢查一次 */
export interface ProjectAccess {
  /** 是成員，或有 project:admin */
  isMember: boolean;
  myRole: ProjectRole | undefined;
  /** 結案或取消：底下的資料都唯讀 */
  closed: boolean;
  /** 修改專案資料、成員、里程碑、標籤、Sprint */
  canManage: boolean;
  /** 改狀態（含重新開啟結案的專案） */
  canChangeStatus: boolean;
  /** 新增、修改任務與留言 */
  canEditTasks: boolean;
  canLogWork: boolean;
}

export interface ProjectContext {
  project: Project | undefined;
  members: ProjectMember[];
  access: ProjectAccess;
  loading: boolean;
  /** 專案不存在或沒有權限 */
  notFound: boolean;
}

/** 一個專案的資料、成員與登入者的權限；專案詳情頁與任務抽屜共用 */
export function useProjectContext(projectId: number): ProjectContext {
  const { user, can } = useAuth();
  const project = useQuery({
    queryKey: ['project', 'detail', projectId],
    queryFn: async () => (await projectApi.query({ ids: [projectId] }))[0] ?? null,
    staleTime: 0,
    refetchOnWindowFocus: 'always',
  });
  const members = useQuery({
    queryKey: ['project-member', 'project', projectId],
    queryFn: () => memberApi.query({ projectId }),
    enabled: project.data != null,
    staleTime: 0,
    refetchOnWindowFocus: 'always',
  });

  const isAdmin = can(PERMISSIONS.PROJECT_ADMIN);
  const myRole = members.data?.find((member) => member.userId === String(user.id))?.role;
  const closed = project.data == null ? false : isProjectClosed(project.data.status);
  const isMember = project.data != null && !project.isError && !members.isError && (isAdmin || myRole !== undefined);
  const isManager = isMember && (isAdmin || myRole === 'MANAGER');
  return {
    project: project.data ?? undefined,
    members: members.data ?? [],
    access: {
      isMember,
      myRole,
      closed,
      canManage: !closed && can(PERMISSIONS.PROJECT_WRITE) && isManager,
      canChangeStatus: can(PERMISSIONS.PROJECT_WRITE) && isManager,
      canEditTasks: !closed && can(PERMISSIONS.TASK_WRITE) && isMember,
      canLogWork: !closed && can(PERMISSIONS.WORK_LOG_WRITE) && isMember,
    },
    loading: project.isPending || (project.data != null && members.isPending),
    notFound: project.isError || project.data === null,
  };
}

/** 專案的全部任務（含子任務）；表格、看板、甘特圖、任務抽屜共用同一份快取 */
export function useProjectTasks(projectId: number) {
  return useQuery({
    queryKey: ['task', 'project', projectId],
    queryFn: () => taskApi.queryAll({ projectId }),
  });
}

/** 專案的里程碑，依到期日排序 */
export function useProjectMilestones(projectId: number) {
  return useQuery({
    queryKey: ['milestone', 'project', projectId],
    queryFn: () => milestoneApi.query({ projectId }),
    select: (milestones) => [...milestones].sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
  });
}

/** 專案的 Sprint，依開始日排序 */
export function useProjectSprints(projectId: number) {
  return useQuery({
    queryKey: ['sprint', 'project', projectId],
    queryFn: () => sprintApi.query({ projectId }),
    select: (sprints) => [...sprints].sort((a, b) => a.startDate.localeCompare(b.startDate) || a.id - b.id),
  });
}

/** 專案的標籤，依名稱排序 */
export function useProjectLabels(projectId: number) {
  return useQuery({
    queryKey: ['project-label', 'project', projectId],
    queryFn: () => labelApi.query({ projectId }),
    select: (labels) => [...labels].sort((a, b) => a.name.localeCompare(b.name)),
  });
}

/** 任務抽屜開在網址的 ?task=id，重新整理或分享連結都會停在同一個任務 */
export function useTaskDrawer() {
  const [params, setParams] = useSearchParams();
  const value = Number(params.get('task'));
  const taskId = Number.isInteger(value) && value > 0 ? value : undefined;
  const open = (id: number): void => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current);
        next.set('task', String(id));
        return next;
      },
      { replace: taskId !== undefined },
    );
  };
  const close = (): void => {
    setParams((current) => {
      const next = new URLSearchParams(current);
      next.delete('task');
      return next;
    });
  };
  return { taskId, open, close };
}
