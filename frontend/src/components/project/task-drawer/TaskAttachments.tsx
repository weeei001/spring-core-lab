import { useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { App, Button, Flex, Image, Popconfirm, Skeleton, Typography, Upload, theme } from 'antd';
import { DeleteOutlined, DownloadOutlined, InboxOutlined, PaperClipOutlined } from '@ant-design/icons';
import { attachmentApi } from '@/api/project/attachment-api';
import type { TaskAttachment } from '@/api/project/attachment.types';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import { useAuth } from '@/core/auth/useAuth';
import { MAX_UPLOAD_FILE_SIZE } from '@/core/http/upload';
import { formatTime } from '@/shared/lib/format';

/** 後端會直接在瀏覽器顯示的圖片類型（其他檔案一律下載） */
const PREVIEW_TYPES = new Set(['image/png', 'image/jpeg', 'image/gif', 'image/webp']);

function sizeText(bytes: number): string {
  if (bytes < 1024) {
    return `${String(bytes)} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${String(Math.round(bytes / 1024))} KB`;
  }
  return `${String(Math.round((bytes / 1024 / 1024) * 10) / 10)} MB`;
}

interface TaskAttachmentsProps {
  taskId: number;
  /** 可以上傳 */
  editable: boolean;
}

/** 任務附件：拖曳或點選上傳，也可以直接 Ctrl + V 貼上截圖；圖片顯示縮圖，點開放大 */
export function TaskAttachments({ taskId, editable }: TaskAttachmentsProps) {
  const { user } = useAuth();
  const { token } = theme.useToken();
  const { message } = App.useApp();
  const attachments = useQuery({ queryKey: ['task-attachment', taskId], queryFn: () => attachmentApi.query(taskId) });
  const directory = useUserDirectory((attachments.data ?? []).map((attachment) => attachment.createId));

  const upload = useMutation({
    mutationFn: (file: File) => attachmentApi.upload(taskId, file),
    onSuccess: (attachment) => {
      void message.success(`已上傳 ${attachment.fileName}`);
    },
    onSettled: () => attachments.refetch(),
  });
  const remove = useMutation({
    mutationFn: (id: number) => attachmentApi.remove(id),
    onSettled: () => attachments.refetch(),
  });

  const send = (file: File): void => {
    if (file.size > MAX_UPLOAD_FILE_SIZE) {
      void message.error(`${file.name} 超過 10MB`);
      return;
    }
    upload.mutate(file);
  };

  // 分頁開著時，貼上的圖片直接上傳（截圖回報 Bug 最常用）
  useEffect(() => {
    if (!editable) {
      return undefined;
    }
    const onPaste = (event: ClipboardEvent): void => {
      const files = Array.from(event.clipboardData?.files ?? []);
      if (files.length === 0) {
        return;
      }
      event.preventDefault();
      for (const file of files) {
        // 截圖貼上時檔名都是 image.png，加上時間方便區分
        const named = file.name === 'image.png' ? new File([file], `screenshot-${String(Date.now())}.png`, { type: file.type }) : file;
        send(named);
      }
    };
    document.addEventListener('paste', onPaste);
    return () => {
      document.removeEventListener('paste', onPaste);
    };
  });

  if (attachments.isPending) {
    return <Skeleton active paragraph={{ rows: 3 }} />;
  }
  const items = attachments.data ?? [];
  const images = items.filter((attachment) => PREVIEW_TYPES.has(attachment.contentType));
  const files = items.filter((attachment) => !PREVIEW_TYPES.has(attachment.contentType));

  const deleteButton = (attachment: TaskAttachment) =>
    editable && attachment.createId === String(user.id) ? (
      <Popconfirm
        title={`刪除 ${attachment.fileName}？`}
        okText="刪除"
        cancelText="取消"
        onConfirm={() => {
          remove.mutate(attachment.id);
        }}
      >
        <Button type="text" size="small" danger icon={<DeleteOutlined />} />
      </Popconfirm>
    ) : null;

  return (
    <Flex vertical gap={16}>
      {editable ? (
        <Upload.Dragger
          multiple
          showUploadList={false}
          disabled={upload.isPending}
          beforeUpload={(file) => {
            send(file);
            // 由 send 自己上傳，不用 antd 的上傳流程
            return false;
          }}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">{upload.isPending ? '上傳中…' : '拖曳檔案到這裡，或點選上傳'}</p>
          <p className="ant-upload-hint">也可以直接 Ctrl + V 貼上截圖；單檔上限 10MB</p>
        </Upload.Dragger>
      ) : null}

      {items.length === 0 ? <Typography.Text type="secondary">沒有附件</Typography.Text> : null}

      {images.length === 0 ? null : (
        <Image.PreviewGroup>
          <Flex gap={12} wrap>
            {images.map((attachment) => (
              <Flex key={attachment.id} vertical gap={4} style={{ width: 120 }}>
                <Image
                  src={attachmentApi.contentUrl(attachment.id)}
                  alt={attachment.fileName}
                  width={120}
                  height={90}
                  style={{ objectFit: 'cover', borderRadius: token.borderRadius, border: `1px solid ${token.colorBorderSecondary}` }}
                />
                <Flex align="center" justify="space-between" gap={4}>
                  <Typography.Text ellipsis={{ tooltip: attachment.fileName }} style={{ fontSize: 12 }}>
                    {attachment.fileName}
                  </Typography.Text>
                  {deleteButton(attachment)}
                </Flex>
              </Flex>
            ))}
          </Flex>
        </Image.PreviewGroup>
      )}

      {files.length === 0 ? null : (
        <Flex vertical>
          {files.map((attachment) => (
            <Flex key={attachment.id} align="center" gap={8} wrap style={{ padding: '6px 0' }}>
              <PaperClipOutlined />
              <Typography.Link href={attachmentApi.contentUrl(attachment.id)} download={attachment.fileName} ellipsis style={{ flex: 1, minWidth: 0 }}>
                {attachment.fileName}
              </Typography.Link>
              <Typography.Text className="task-detail-attachment-meta" type="secondary" style={{ fontSize: 12 }}>
                {sizeText(attachment.size)} · {directory.name(attachment.createId)} · {formatTime(attachment.createTime)}
              </Typography.Text>
              <Typography.Link href={attachmentApi.contentUrl(attachment.id)} download={attachment.fileName} title="下載">
                <DownloadOutlined />
              </Typography.Link>
              {deleteButton(attachment)}
            </Flex>
          ))}
        </Flex>
      )}
    </Flex>
  );
}
