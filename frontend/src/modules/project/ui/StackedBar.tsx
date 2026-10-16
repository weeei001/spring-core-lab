import { Flex, Tooltip, Typography, theme } from 'antd';
import { motion } from 'motion/react';

export interface StackedSegment {
  key: string;
  label: string;
  value: number;
  color: string;
}

/**
 * 一條橫向堆疊長條（例如任務狀態分布）：各段之間留 2px 空隙，滑過顯示數量與比例；
 * 底下一定有圖例（顏色 + 文字 + 數量），不只靠顏色辨識。
 */
export function StackedBar({ segments }: { segments: readonly StackedSegment[] }) {
  const { token } = theme.useToken();
  const total = segments.reduce((sum, segment) => sum + segment.value, 0);
  const visible = segments.filter((segment) => segment.value > 0);
  return (
    <Flex vertical gap={10}>
      <Flex gap={2} style={{ height: 14, borderRadius: 4, overflow: 'hidden', background: token.colorFillTertiary }}>
        {visible.map((segment, index) => (
          <Tooltip key={segment.key} title={`${segment.label}：${String(segment.value)}（${String(Math.round((segment.value / total) * 100))}%）`}>
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: `${String((segment.value / total) * 100)}%` }}
              transition={{ duration: 0.6, delay: index * 0.08, ease: 'easeOut' }}
              style={{ height: '100%', background: segment.color, minWidth: 4 }}
            />
          </Tooltip>
        ))}
      </Flex>
      <Flex gap={16} wrap>
        {segments.map((segment) => (
          <Flex key={segment.key} align="center" gap={6}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: segment.color }} />
            <Typography.Text type="secondary">{segment.label}</Typography.Text>
            <Typography.Text strong>{String(segment.value)}</Typography.Text>
          </Flex>
        ))}
      </Flex>
    </Flex>
  );
}
