import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Button, Flex, Popconfirm, Table, Tag, Typography, message, type TableColumnsType } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { useUserNames } from '@/modules/basic-data';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { useSaveDelete } from '@/shared/hooks/useSaveDelete';
import { transColumn } from '@/shared/ui/auditColumn';
import { useTagOptions } from '../tag/useTagOptions';
import { CATEGORY_LABELS } from './labels';
import { productApi } from './product-api';
import type { Product, ProductQuery } from './product.types';
import { ProductFilters } from './ProductFilters';
import { ProductFormModal, type ProductFormValues } from './ProductFormModal';
import { StockModal } from './StockModal';

type Editing = { mode: 'create' } | { mode: 'edit'; product: Product } | null;

interface AdjustStockInput {
  id: number;
  quantity: number;
}

/** 商品維護（查詢需要 product:read；新增、修改、刪除需要 product:write；調整庫存需要 product:stock） */
export function ProductPage() {
  const { can } = useAuth();
  const writable = can(PERMISSIONS.PRODUCT_WRITE);
  const stockAdjustable = can(PERMISSIONS.PRODUCT_STOCK);
  const queryClient = useQueryClient();
  const list = usePagedList<Product, ProductQuery>('product', productApi.queryPage, {});
  const { save, remove } = useSaveDelete<Product>('product', productApi);
  const userName = useUserNames(list.rows);
  const { names: tagNames } = useTagOptions();
  const [editing, setEditing] = useState<Editing>(null);
  const [stockTarget, setStockTarget] = useState<Product | null>(null);

  const adjustStock = useMutation({
    mutationFn: ({ id, quantity }: AdjustStockInput) => productApi.adjustStock(id, quantity),
    onSuccess: () => {
      void message.success('已調整庫存');
      setStockTarget(null);
      return queryClient.invalidateQueries({ queryKey: ['product'] });
    },
  });

  const close = (): void => {
    setEditing(null);
  };

  const submit = (values: ProductFormValues): void => {
    if (editing === null) {
      return;
    }
    const common = {
      sku: values.sku,
      name: values.name,
      category: values.category,
      price: values.price,
      onSale: values.onSale,
      brandCode: values.brandCode,
      tagIds: values.tagIds ?? [],
    };
    if (editing.mode === 'create') {
      save.mutate({ values: { ...common, stock: values.stock } }, { onSuccess: close });
    } else {
      // 系統編號不可修改、庫存要用調整庫存，都不送
      save.mutate({ id: editing.product.id, values: common }, { onSuccess: close });
    }
  };

  const columns: TableColumnsType<Product> = [
    { title: '系統編號', dataIndex: 'code', width: 130 },
    { title: '貨號', dataIndex: 'sku' },
    { title: '名稱', dataIndex: 'name' },
    { title: '分類', key: 'category', render: (_value, product) => CATEGORY_LABELS[product.category] },
    { title: '品牌', key: 'brand', render: (_value, product) => product.brand?.name ?? '-' },
    {
      title: '標籤',
      key: 'tags',
      render: (_value, product) => (
        <Flex wrap gap={4}>
          {product.tagIds.map((id) => (
            <Tag key={id}>{tagNames.get(id) ?? String(id)}</Tag>
          ))}
        </Flex>
      ),
    },
    { title: '售價', key: 'price', align: 'right', render: (_value, product) => product.price.toLocaleString('zh-TW') },
    { title: '庫存', dataIndex: 'stock', align: 'right' },
    {
      title: '狀態',
      key: 'onSale',
      render: (_value, product) => (product.onSale ? <Tag color="green">上架</Tag> : <Tag>下架</Tag>),
    },
    transColumn<Product>(userName),
  ];
  if (writable || stockAdjustable) {
    columns.push({
      title: '操作',
      key: 'actions',
      width: 190,
      render: (_value, product) => (
        <Flex gap={4}>
          {writable && (
            <Button
              type="link"
              size="small"
              onClick={() => {
                setEditing({ mode: 'edit', product });
              }}
            >
              編輯
            </Button>
          )}
          {stockAdjustable && (
            <Button
              type="link"
              size="small"
              onClick={() => {
                setStockTarget(product);
              }}
            >
              庫存
            </Button>
          )}
          {writable && (
            <Popconfirm
              title="確定刪除這個商品？"
              okText="刪除"
              cancelText="取消"
              onConfirm={() => {
                remove.mutate(product.id);
              }}
            >
              <Button type="link" size="small" danger>
                刪除
              </Button>
            </Popconfirm>
          )}
        </Flex>
      ),
    });
  }

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center">
        <Typography.Title level={4} style={{ margin: 0 }}>
          商品
        </Typography.Title>
        {writable && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditing({ mode: 'create' });
            }}
          >
            新增商品
          </Button>
        )}
      </Flex>
      <ProductFilters onSearch={list.search} />
      <Table<Product>
        rowKey="id"
        columns={columns}
        dataSource={list.rows}
        loading={list.loading}
        pagination={list.pagination}
        scroll={{ x: 'max-content' }}
      />

      <ProductFormModal
        open={editing !== null}
        product={editing?.mode === 'edit' ? editing.product : null}
        saving={save.isPending}
        onCancel={close}
        onSubmit={submit}
      />
      <StockModal
        product={stockTarget}
        saving={adjustStock.isPending}
        onCancel={() => {
          setStockTarget(null);
        }}
        onSubmit={(quantity) => {
          if (stockTarget !== null) {
            adjustStock.mutate({ id: stockTarget.id, quantity });
          }
        }}
      />
    </Flex>
  );
}
