import { Button, Result } from 'antd';
import { ApiError } from '@/core/http/client';

export function TaskRequestError({ error, onRetry }: { error: Error; onRetry: () => void }) {
  const status = error instanceof ApiError ? error.status : undefined;
  if (status === 403) {
    return <Result status="403" title="無權限查看任務" subTitle="你目前沒有這個專案或任務的存取權限。" />;
  }
  if (status === 404) {
    return <Result status="404" title="找不到任務" subTitle="任務或所屬專案已不存在。" />;
  }
  return (
    <Result
      status="error"
      title="無法載入任務"
      subTitle="請求失敗，請稍後重試。"
      extra={<Button onClick={onRetry}>重試</Button>}
    />
  );
}
