import { Flex, Typography, theme } from 'antd';

interface BurndownChartProps {
  /** x 軸的每一天，YYYY-MM-DD，含頭尾 */
  days: readonly string[];
  /** 每天結束時剩下的量；還沒到的日子為 null */
  remaining: readonly (number | null)[];
  /** 理想線的起點（第一天的總量），最後一天降到 0 */
  total: number;
  /** 數值單位，例如「點」「個任務」 */
  unit: string;
}

const WIDTH = 640;
const HEIGHT = 220;
const PAD = { top: 12, right: 12, bottom: 28, left: 36 };

/** 燃盡圖：灰色虛線是理想進度，主色實線是實際剩餘量 */
export function BurndownChart({ days, remaining, total, unit }: BurndownChartProps) {
  const { token } = theme.useToken();
  const max = Math.max(total, ...remaining.map((value) => value ?? 0), 1);
  const innerWidth = WIDTH - PAD.left - PAD.right;
  const innerHeight = HEIGHT - PAD.top - PAD.bottom;
  const x = (index: number): number => PAD.left + (days.length <= 1 ? 0 : (index / (days.length - 1)) * innerWidth);
  const y = (value: number): number => PAD.top + innerHeight - (value / max) * innerHeight;

  const actual = remaining
    .map((value, index) => (value === null ? null : `${String(x(index))},${String(y(value))}`))
    .filter((point): point is string => point !== null)
    .join(' ');
  // x 軸最多標 7 個日期，避免擠在一起
  const step = Math.max(1, Math.ceil(days.length / 7));
  const lastIndex = remaining.reduce<number>((last, value, index) => (value === null ? last : index), -1);

  return (
    <Flex vertical gap={4}>
      <svg viewBox={`0 0 ${String(WIDTH)} ${String(HEIGHT)}`} style={{ width: '100%', height: 'auto' }} role="img" aria-label="燃盡圖">
        {[0, 0.5, 1].map((ratio) => {
          const value = Math.round(max * ratio);
          return (
            <g key={ratio}>
              <line x1={PAD.left} x2={WIDTH - PAD.right} y1={y(value)} y2={y(value)} stroke={token.colorBorderSecondary} />
              <text x={PAD.left - 6} y={y(value) + 4} textAnchor="end" fontSize={11} fill={token.colorTextSecondary}>
                {value}
              </text>
            </g>
          );
        })}
        {days.map((day, index) =>
          index % step === 0 || index === days.length - 1 ? (
            <text key={day} x={x(index)} y={HEIGHT - 8} textAnchor="middle" fontSize={11} fill={token.colorTextSecondary}>
              {day.slice(5)}
            </text>
          ) : null,
        )}
        <line x1={x(0)} y1={y(total)} x2={x(days.length - 1)} y2={y(0)} stroke={token.colorTextQuaternary} strokeDasharray="4 4" strokeWidth={1.5} />
        {actual === '' ? null : <polyline points={actual} fill="none" stroke={token.colorPrimary} strokeWidth={2} />}
        {lastIndex < 0 ? null : <circle cx={x(lastIndex)} cy={y(remaining[lastIndex] ?? 0)} r={4} fill={token.colorPrimary} />}
      </svg>
      <Flex gap={16} justify="center">
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          <span style={{ color: token.colorTextQuaternary }}>- - -</span> 理想
        </Typography.Text>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          <span style={{ color: token.colorPrimary }}>━</span> 剩餘（{unit}）
        </Typography.Text>
      </Flex>
    </Flex>
  );
}
