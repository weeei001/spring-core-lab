import { useQuery } from '@tanstack/react-query';
import { brandApi } from './brand-api';

/** 品牌下拉選單；value 是品牌 code（商品以 code 關聯品牌） */
export function useBrandOptions(): { value: string; label: string }[] {
  const { data } = useQuery({ queryKey: ['brand', 'options'], queryFn: () => brandApi.query({}) });
  return (data ?? []).map((brand) => ({ value: brand.code, label: brand.name }));
}
