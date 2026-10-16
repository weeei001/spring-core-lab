import { Typography, type TableColumnType } from 'antd';
import type { BaseEntity } from '@/core/http/types';
import { formatTime } from '@/shared/lib/format';

/** 表格的「最後修改」欄：修改者名字與時間；userName 把 user id 換成名字 */
// T 只出現在回傳型別：由呼叫端指定表格的資料型別，例如 transColumn<Project>(userName)
 
export function transColumn<T extends BaseEntity>(userName: (id: string) => string): TableColumnType<T> {
  return {
    title: '最後修改',
    key: 'trans',
    width: 160,
    render: (_value, row) => (
      <>
        {userName(row.transId)}
        <br />
        <Typography.Text type="secondary">{formatTime(row.transTime)}</Typography.Text>
      </>
    ),
  };
}
