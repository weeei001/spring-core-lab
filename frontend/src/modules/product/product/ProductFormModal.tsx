import { Form, Input, InputNumber, Select, Switch } from 'antd';
import { toOptions } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';
import { useBrandOptions } from '../brand/useBrandOptions';
import { useTagOptions } from '../tag/useTagOptions';
import { CATEGORY_LABELS } from './labels';
import type { Product, ProductCategory } from './product.types';

export interface ProductFormValues {
  sku: string;
  name: string;
  category: ProductCategory;
  price: number;
  /** 只有新增時填寫，之後用調整庫存修改 */
  stock?: number;
  onSale: boolean;
  brandCode?: string;
  tagIds?: number[];
}

interface ProductFormModalProps {
  open: boolean;
  /** null 為新增 */
  product: Product | null;
  saving: boolean;
  onCancel: () => void;
  onSubmit: (values: ProductFormValues) => void;
}

/** 新增／修改商品；系統編號由後端取號，庫存只在新增時填寫 */
export function ProductFormModal({ open, product, saving, onCancel, onSubmit }: ProductFormModalProps) {
  const brandOptions = useBrandOptions();
  const { options: tagOptions } = useTagOptions();
  const creating = product === null;

  return (
    <FormModal<ProductFormValues>
      open={open}
      title={product === null ? '新增商品' : `修改商品：${product.code}`}
      initialValues={
        product === null
          ? { onSale: true, tagIds: [] }
          : {
              sku: product.sku,
              name: product.name,
              category: product.category,
              price: product.price,
              onSale: product.onSale,
              brandCode: product.brandCode ?? undefined,
              tagIds: product.tagIds,
            }
      }
      saving={saving}
      onCancel={onCancel}
      onSubmit={onSubmit}
    >
      <Form.Item
        name="sku"
        label="貨號"
        rules={[
          { required: true, message: '請輸入貨號' },
          { pattern: /^[A-Za-z0-9-]+$/, message: '只能使用英文、數字與連字號' },
          { max: 50 },
        ]}
      >
        <Input />
      </Form.Item>
      <Form.Item name="name" label="名稱" rules={[{ required: true, whitespace: true, message: '請輸入名稱' }, { max: 100 }]}>
        <Input />
      </Form.Item>
      <Form.Item name="category" label="分類" rules={[{ required: true, message: '請選擇分類' }]}>
        <Select options={toOptions(CATEGORY_LABELS)} />
      </Form.Item>
      <Form.Item name="price" label="售價" rules={[{ required: true, message: '請輸入售價' }]}>
        <InputNumber min={0} max={99999999.99} precision={2} style={{ width: '100%' }} />
      </Form.Item>
      {creating && (
        <Form.Item name="stock" label="初始庫存" rules={[{ required: true, message: '請輸入初始庫存' }]}>
          <InputNumber min={0} precision={0} style={{ width: '100%' }} />
        </Form.Item>
      )}
      <Form.Item name="onSale" label="上架" valuePropName="checked">
        <Switch />
      </Form.Item>
      <Form.Item name="brandCode" label="品牌" extra={creating ? undefined : '設定後無法清空，只能換成其他品牌'}>
        <Select options={brandOptions} allowClear={creating} placeholder="選填" />
      </Form.Item>
      <Form.Item name="tagIds" label="標籤" extra="最多 20 個">
        <Select mode="multiple" options={tagOptions} placeholder="選填" maxCount={20} />
      </Form.Item>
    </FormModal>
  );
}
