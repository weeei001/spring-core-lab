import type { ReactNode } from 'react';
import { Card, Flex, Typography, theme } from 'antd';
import { motion } from 'motion/react';
import { CountUp } from './CountUp';

interface StatTileProps {
  title: string;
  value: number;
  icon: ReactNode;
  /** 圖示底色 */
  color: string;
  decimals?: number;
  suffix?: string;
  /** 依序進場的延遲（秒） */
  delay?: number;
  onClick?: () => void;
}

/** 儀表板、專案概覽的數字卡片：進場淡入、滑過浮起、數字跑動 */
export function StatTile({ title, value, icon, color, decimals, suffix, delay = 0, onClick }: StatTileProps) {
  const { token } = theme.useToken();
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      // 進場的延遲只放在 animate，滑過浮起才不會跟著延遲
      animate={{ opacity: 1, y: 0, transition: { duration: 0.35, delay } }}
      whileHover={{ y: -3, transition: { duration: 0.15 } }}
      style={{ flex: '1 1 180px', cursor: onClick === undefined ? 'default' : 'pointer' }}
      onClick={onClick}
    >
      <Card styles={{ body: { padding: 16 } }}>
        <Flex align="center" gap={12}>
          <Flex
            align="center"
            justify="center"
            style={{
              width: 44,
              height: 44,
              borderRadius: token.borderRadiusLG,
              background: `${color}1f`,
              color,
              fontSize: 20,
            }}
          >
            {icon}
          </Flex>
          <Flex vertical>
            <Typography.Text type="secondary">{title}</Typography.Text>
            <Typography.Text style={{ fontSize: 24, fontWeight: 600, lineHeight: 1.2 }}>
              <CountUp value={value} decimals={decimals} />
              {suffix === undefined ? null : <span style={{ fontSize: 14, marginInlineStart: 4 }}>{suffix}</span>}
            </Typography.Text>
          </Flex>
        </Flex>
      </Card>
    </motion.div>
  );
}
