import { useMutation } from '@tanstack/react-query';
import { App, Button, Card, Checkbox, Flex, Form, Input, Typography } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import { notificationApi } from '@/api/notification/notification-api';
import type { AnnouncementRequest } from '@/api/notification/notification.types';
import { UserMultiSelect } from '@/components/user/UserMultiSelect';
import { blankToUndefined } from '@/shared/lib/format';

const MAX_RECIPIENTS = 100;

type AnnouncementFormValues = AnnouncementRequest;

/** 發公告給指定的使用者：收件人會收到站內通知，勾選 Email 時同時寄信（需要 notification:send，由路由檢查） */
export function AnnouncementPage() {
  const [form] = Form.useForm<AnnouncementFormValues>();
  const { message } = App.useApp();
  // 失敗訊息由 app/query-client 統一顯示
  const announce = useMutation({
    mutationFn: notificationApi.announce,
    onSuccess: (notifications) => {
      void message.success(`已發送給 ${String(notifications.length)} 位使用者`);
      form.resetFields();
    },
  });

  const submit = (values: AnnouncementFormValues): void => {
    announce.mutate({ ...values, title: values.title.trim(), content: blankToUndefined(values.content) });
  };

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4}>發送公告</Typography.Title>
      <Card style={{ maxWidth: 720 }}>
        <Form<AnnouncementFormValues>
          form={form}
          layout="vertical"
          initialValues={{ recipientIds: [], email: false }}
          onFinish={submit}
        >
          <Form.Item
            name="recipientIds"
            label="收件人"
            extra={`最多 ${String(MAX_RECIPIENTS)} 位`}
            rules={[
              { required: true, type: 'array', min: 1, message: '請選擇收件人' },
              { type: 'array', max: MAX_RECIPIENTS, message: `最多 ${String(MAX_RECIPIENTS)} 位收件人` },
            ]}
          >
            <UserMultiSelect />
          </Form.Item>
          <Form.Item name="title" label="標題" rules={[{ required: true, whitespace: true, message: '請輸入標題' }, { max: 200 }]}>
            <Input placeholder="例如 系統維護公告" />
          </Form.Item>
          <Form.Item name="content" label="內容" rules={[{ max: 2000 }]}>
            <Input.TextArea rows={6} showCount maxLength={2000} />
          </Form.Item>
          <Form.Item name="email" valuePropName="checked" extra="沒有設定 Email 的收件人只會收到站內通知">
            <Checkbox>同時寄送 Email</Checkbox>
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={announce.isPending}>
            發送
          </Button>
        </Form>
      </Card>
    </Flex>
  );
}
