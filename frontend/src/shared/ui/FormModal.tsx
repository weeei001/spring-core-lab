import type { ReactNode } from 'react';
import { Form, Modal, type FormProps } from 'antd';

interface FormModalProps<V> {
  open: boolean;
  title: string;
  initialValues?: FormProps<V>['initialValues'];
  saving: boolean;
  onCancel: () => void;
  onSubmit: (values: V) => void;
  /** 表單欄位（Form.Item） */
  children: ReactNode;
}

/** 新增／修改用的彈窗表單；關閉時銷毀，下次打開會重新帶入 initialValues */
export function FormModal<V>({ open, title, initialValues, saving, onCancel, onSubmit, children }: FormModalProps<V>) {
  const [form] = Form.useForm<V>();
  return (
    <Modal
      open={open}
      title={title}
      okText="儲存"
      cancelText="取消"
      confirmLoading={saving}
      onOk={() => {
        form.submit();
      }}
      onCancel={onCancel}
      destroyOnHidden
    >
      <Form<V> form={form} layout="vertical" initialValues={initialValues} onFinish={onSubmit} preserve={false}>
        {children}
      </Form>
    </Modal>
  );
}
