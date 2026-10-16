import { Flex, Tooltip, Typography, theme } from 'antd';

export interface BarDatum {
  key: string;
  /** x 軸文字 */
  label: string;
  value: number;
  /** 滑過時的說明 */
  tooltip: string;
  /** 強調這一根（例如今天、本週） */
  highlight?: boolean;
}

interface BarChartProps {
  data: readonly BarDatum[];
  height?: number;
  /** 長條上方的數值文字 */
  format: (value: number) => string;
}

/**
 * 單一數列的直條圖（例如每週工時）：一個顏色、貼齊基線、長條間留空隙，
 * 數值直接標在長條上，滑過顯示說明。
 */
export function BarChart({ data, height = 160, format }: BarChartProps) {
  const { token } = theme.useToken();
  const max = Math.max(...data.map((datum) => datum.value), 0);
  return (
    <Flex vertical gap={4}>
      <Flex align="flex-end" gap={8} style={{ height, borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
        {data.map((datum) => {
          const ratio = max === 0 ? 0 : datum.value / max;
          return (
            <Tooltip key={datum.key} title={datum.tooltip}>
              <Flex vertical align="center" justify="flex-end" style={{ flex: 1, height: '100%', cursor: 'default' }}>
                {datum.value > 0 ? (
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {format(datum.value)}
                  </Typography.Text>
                ) : null}
                <div
                  style={{
                    height: `${String(Math.round(ratio * 80))}%`,
                    width: '70%',
                    maxWidth: 36,
                    minHeight: datum.value > 0 ? 2 : 0,
                    background: datum.highlight === true ? token.colorPrimary : token.colorPrimaryBorder,
                  }}
                />
              </Flex>
            </Tooltip>
          );
        })}
      </Flex>
      <Flex gap={8}>
        {data.map((datum) => (
          <Typography.Text
            key={datum.key}
            type={datum.highlight === true ? undefined : 'secondary'}
            strong={datum.highlight === true}
            style={{ flex: 1, textAlign: 'center', fontSize: 12 }}
          >
            {datum.label}
          </Typography.Text>
        ))}
      </Flex>
    </Flex>
  );
}
