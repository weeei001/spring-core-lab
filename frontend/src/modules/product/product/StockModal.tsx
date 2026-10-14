import { Form, InputNumber, Typography } from 'antd';
import { FormModal } from '@/shared/ui/FormModal';
import type { Product } from './product.types';

interface StockFormValues {
  quantity: number;
}

interface StockModalProps {
  /** null 時關閉 */
  product: Product | null;
  saving: boolean;
  onCancel: () => void;
  onSubmit: (quantity: number) => void;
}

/** 調整庫存：正數為入庫、負數為出庫 */
export function StockModal({ product, saving, onCancel, onSubmit }: StockModalProps) {
  return (
    <FormModal<StockFormValues>
      open={product !== null}
      title={`調整庫存：${product?.name ?? ''}`}
      saving={saving}
      onCancel={onCancel}
      onSubmit={(values) => {
        onSubmit(values.quantity);
      }}
    >
      <Typography.Paragraph>目前庫存：{product?.stock ?? 0}</Typography.Paragraph>
      <Form.Item
        name="quantity"
        label="數量"
        extra="正數為入庫、負數為出庫；庫存不足時無法出庫"
        rules={[
          { required: true, message: '請輸入數量' },
          {
            validator: (_rule, value: number | null) =>
              value === 0 ? Promise.reject(new Error('數量不可為 0')) : Promise.resolve(),
          },
        ]}
      >
        <InputNumber precision={0} style={{ width: '100%' }} />
      </Form.Item>
    </FormModal>
  );
}
