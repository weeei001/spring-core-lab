import { Button, Flex, Form, Input, InputNumber, Select } from 'antd';
import { blankToUndefined, toOptions } from '@/shared/lib/format';
import { useBrandOptions } from '../brand/useBrandOptions';
import { useTagOptions } from '../tag/useTagOptions';
import { CATEGORY_LABELS } from './labels';
import type { ProductCategory, ProductQuery } from './product.types';

interface FilterValues {
  keyword?: string;
  categories?: ProductCategory[];
  brandCode?: string;
  tagId?: number;
  onSale?: boolean;
  minPrice?: number | null;
  maxPrice?: number | null;
}

const ON_SALE_OPTIONS = [
  { value: true, label: '上架' },
  { value: false, label: '下架' },
];

/** 商品查詢條件；按查詢後轉成後端的 ProductQuery */
export function ProductFilters({ onSearch }: { onSearch: (query: ProductQuery) => void }) {
  const [form] = Form.useForm<FilterValues>();
  const brandOptions = useBrandOptions();
  const { options: tagOptions } = useTagOptions();

  const submit = (values: FilterValues): void => {
    onSearch({
      keyword: blankToUndefined(values.keyword),
      categories: values.categories,
      brandCode: values.brandCode,
      tagId: values.tagId,
      onSale: values.onSale,
      minPrice: values.minPrice ?? undefined,
      maxPrice: values.maxPrice ?? undefined,
    });
  };

  return (
    <Form<FilterValues> form={form} layout="inline" onFinish={submit} style={{ rowGap: 12 }}>
      <Form.Item name="keyword">
        <Input placeholder="編號、貨號或名稱" allowClear style={{ width: 200 }} />
      </Form.Item>
      <Form.Item name="categories">
        <Select mode="multiple" placeholder="分類" options={toOptions(CATEGORY_LABELS)} allowClear style={{ minWidth: 160 }} />
      </Form.Item>
      <Form.Item name="brandCode">
        <Select placeholder="品牌" options={brandOptions} allowClear style={{ width: 140 }} />
      </Form.Item>
      <Form.Item name="tagId">
        <Select placeholder="標籤" options={tagOptions} allowClear style={{ width: 140 }} />
      </Form.Item>
      <Form.Item name="onSale">
        <Select placeholder="狀態" options={ON_SALE_OPTIONS} allowClear style={{ width: 110 }} />
      </Form.Item>
      <Form.Item name="minPrice">
        <InputNumber placeholder="最低價" min={0} />
      </Form.Item>
      <Form.Item name="maxPrice">
        <InputNumber placeholder="最高價" min={0} />
      </Form.Item>
      <Form.Item>
        <Flex gap={8}>
          <Button type="primary" htmlType="submit">
            查詢
          </Button>
          <Button
            onClick={() => {
              form.resetFields();
              form.submit();
            }}
          >
            清除
          </Button>
        </Flex>
      </Form.Item>
    </Form>
  );
}
