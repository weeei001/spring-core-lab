import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Select } from 'antd';
import { userApi } from '@/api/basic-data/user-api';
import type { User } from '@/api/basic-data/user.types';
import { useDebounced } from '@/shared/hooks/useDebounced';
import { useUserDirectory } from './useUserDirectory';

interface UserMultiSelectProps {
  value?: string[];
  onChange?: (value: string[]) => void;
  placeholder?: string;
}

function toOption(user: User) {
  return { value: String(user.id), label: user.email === null ? user.name : `${user.name}（${user.email}）` };
}

/**
 * 可選多位使用者的 UserSelect，值為 user id（字串）陣列；可放在 Form.Item 內。
 * 換關鍵字後已選的人不在搜尋結果裡，改用 useUserDirectory 查回名字，才不會只顯示 id。
 */
export function UserMultiSelect({ value = [], onChange, placeholder = '輸入名稱、帳號或 Email 搜尋' }: UserMultiSelectProps) {
  const [keyword, setKeyword] = useState('');
  const trimmed = useDebounced(keyword.trim());
  const users = useQuery({
    queryKey: ['user', 'search', trimmed],
    queryFn: () => userApi.query({ keyword: trimmed }),
    enabled: trimmed !== '',
    staleTime: 60 * 1000,
  });
  const directory = useUserDirectory(value);
  const found = (users.data ?? []).map(toOption);
  const foundIds = new Set(found.map((option) => option.value));
  const selected = value
    .filter((id) => !foundIds.has(id))
    .map((id) => {
      const user = directory.user(id);
      return user === undefined ? { value: id, label: id } : toOption(user);
    });
  return (
    <Select
      mode="multiple"
      value={value}
      onChange={(ids) => {
        setKeyword('');
        onChange?.(ids);
      }}
      options={[...selected, ...found]}
      placeholder={placeholder}
      loading={users.isFetching}
      showSearch={{ filterOption: false, searchValue: keyword, onSearch: setKeyword }}
      notFoundContent={trimmed === '' ? '請輸入關鍵字' : '找不到使用者'}
      style={{ width: '100%' }}
    />
  );
}
