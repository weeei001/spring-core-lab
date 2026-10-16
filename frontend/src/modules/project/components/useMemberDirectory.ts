import { useUserDirectory, type UserDirectory } from '@/modules/basic-data';
import type { ProjectMember } from '../api/member.types';

export interface MemberDirectory {
  directory: UserDirectory;
  /** 指派人下拉選單的選項，值為 user id */
  options: { value: string; label: string }[];
}

/**
 * 成員的名字與下拉選項；extraIds 是畫面上還會用到、但不一定是成員的人（例如已移除成員的留言者）。
 */
export function useMemberDirectory(members: readonly ProjectMember[], extraIds: readonly (string | null)[] = []): MemberDirectory {
  const directory = useUserDirectory([...members.map((member) => member.userId), ...extraIds]);
  const options = members.map((member) => ({ value: member.userId, label: directory.name(member.userId) }));
  return { directory, options };
}
