import { Flex } from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import { PROJECT_COLORS } from './labels';

interface ColorSwatchesProps {
  value?: string;
  onChange?: (value: string) => void;
}

/** 專案顏色選擇；可放在 Form.Item 內 */
export function ColorSwatches({ value, onChange }: ColorSwatchesProps) {
  return (
    <Flex gap={8} wrap>
      {PROJECT_COLORS.map((color) => {
        const selected = value?.toUpperCase() === color;
        return (
          <button
            key={color}
            type="button"
            aria-label={color}
            onClick={() => {
              onChange?.(color);
            }}
            style={{
              width: 28,
              height: 28,
              borderRadius: '50%',
              border: selected ? '2px solid rgba(0,0,0,0.45)' : '2px solid transparent',
              background: color,
              color: '#fff',
              cursor: 'pointer',
              transform: selected ? 'scale(1.1)' : 'none',
              transition: 'transform 0.15s',
            }}
          >
            {selected ? <CheckOutlined /> : null}
          </button>
        );
      })}
    </Flex>
  );
}
