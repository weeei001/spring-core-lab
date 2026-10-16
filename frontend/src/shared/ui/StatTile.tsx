import { Card, Statistic } from 'antd';

interface StatTileProps {
  title: string;
  value: number;
  decimals?: number;
  suffix?: string;
  onClick?: () => void;
}

/** 儀表板、專案概覽的數字卡片：標題加大數字；可點時整張卡片可點 */
export function StatTile({ title, value, decimals, suffix, onClick }: StatTileProps) {
  return (
    <Card hoverable={onClick !== undefined} onClick={onClick} style={{ flex: '1 1 180px' }}>
      <Statistic title={title} value={value} precision={decimals} suffix={suffix} />
    </Card>
  );
}
