import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Select } from 'antd';
import { useDebounced } from '@/shared/hooks/useDebounced';
import { userApi } from './user-api';

interface UserSelectProps {
  value?: string;
  onChange?: (value: string) => void;
  /** 不顯示的使用者，例如已經是成員的人 */
  excludeIds?: readonly string[];
  placeholder?: string;
}

/** 輸入名稱、帳號或 Email 搜尋使用者，值為 user id（字串）；可放在 Form.Item 內 */
export function UserSelect({ value, onChange, excludeIds = [], placeholder = '輸入名稱、帳號或 Email 搜尋' }: UserSelectProps) {
  const [keyword, setKeyword] = useState('');
  const trimmed = useDebounced(keyword.trim());
  const users = useQuery({
    queryKey: ['user', 'search', trimmed],
    queryFn: () => userApi.query({ keyword: trimmed }),
    enabled: trimmed !== '',
    staleTime: 60 * 1000,
  });
  const options = (users.data ?? [])
    .filter((user) => !excludeIds.includes(String(user.id)))
    .map((user) => ({
      value: String(user.id),
      label: user.email === null ? user.name : `${user.name}（${user.email}）`,
    }));
  return (
    <Select
      value={value}
      onChange={onChange}
      options={options}
      placeholder={placeholder}
      loading={users.isFetching}
      showSearch={{ filterOption: false, onSearch: setKeyword }}
      notFoundContent={trimmed === '' ? '請輸入關鍵字' : '找不到使用者'}
      style={{ width: '100%' }}
    />
  );
}
