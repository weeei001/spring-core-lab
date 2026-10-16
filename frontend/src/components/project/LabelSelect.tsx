import type { CSSProperties } from 'react';
import { Select, Tag } from 'antd';
import type { ProjectLabel } from '@/api/project/label.types';

interface LabelSelectProps {
  labels: readonly ProjectLabel[];
  value?: number[];
  onChange?: (value: number[]) => void;
  disabled?: boolean;
  placeholder?: string;
  style?: CSSProperties;
}

/** 標籤多選，選項與已選的標籤都用標籤顏色顯示；可放在 Form.Item 內 */
export function LabelSelect({ labels, value, onChange, disabled, placeholder = '沒有標籤', style }: LabelSelectProps) {
  const colors = new Map(labels.map((label) => [label.id, label.color] as const));
  return (
    <Select<number[]>
      mode="multiple"
      allowClear
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      style={{ width: '100%', ...style }}
      options={labels.map((label) => ({ value: label.id, label: label.name }))}
      showSearch={{ optionFilterProp: 'label' }}
      notFoundContent={labels.length === 0 ? '專案還沒有標籤，請專案經理到「標籤」分頁新增' : undefined}
      optionRender={(option) => <Tag color={colors.get(Number(option.value))}>{option.label}</Tag>}
      tagRender={({ label, value: id, closable, onClose }) => (
        <Tag color={colors.get(Number(id))} closable={closable} onClose={onClose} style={{ marginInlineEnd: 4 }}>
          {label}
        </Tag>
      )}
      onChange={(next) => {
        onChange?.(next);
      }}
    />
  );
}
