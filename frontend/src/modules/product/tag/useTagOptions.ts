import { useQuery } from '@tanstack/react-query';
import { tagApi } from './tag-api';

/** 標籤下拉選單，以及 id 換名稱的對照 */
export function useTagOptions(): { options: { value: number; label: string }[]; names: Map<number, string> } {
  const { data } = useQuery({ queryKey: ['tag', 'options'], queryFn: () => tagApi.query({}) });
  const tags = data ?? [];
  return {
    options: tags.map((tag) => ({ value: tag.id, label: tag.name })),
    names: new Map(tags.map((tag) => [tag.id, tag.name] as const)),
  };
}
