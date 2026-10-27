import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Flex, Form, Input, Modal, Table } from 'antd';
import { Link, useNavigate } from 'react-router';
import { manualQaApi } from '@/api/project/manual-qa-api';
import type { QaBatch, QaBatchInput } from '@/api/project/manual-qa.types';
import type { ProjectAccess } from '@/components/project/hooks';

export function ManualQaTab({ projectId, access }: { projectId: number; access: ProjectAccess }) {
  const [page, setPage] = useState(1);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<QaBatchInput>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const batches = useQuery({
    queryKey: ['manual-qa', 'batches', projectId, page],
    queryFn: () => manualQaApi.batches(projectId, page),
  });
  const create = useMutation({
    mutationFn: (body: QaBatchInput) => manualQaApi.create(projectId, body),
    onSuccess: (batch) => {
      void queryClient.invalidateQueries({ queryKey: ['manual-qa', 'batches', projectId] });
      setOpen(false);
      form.resetFields();
      void navigate(`/projects/${String(projectId)}/qa/${String(batch.id)}`);
    },
  });
  return (
    <Flex vertical gap={16}>
      {access.canEditManualQa ? (
        <Button
          type="primary"
          onClick={() => {
            setOpen(true);
          }}
        >
          建立驗收批次
        </Button>
      ) : null}
      {batches.error ? (
        <Alert
          type="error"
          title={batches.error.message}
          action={
            <Button
              onClick={() => {
                void batches.refetch();
              }}
            >
              重試
            </Button>
          }
        />
      ) : null}
      <Table<QaBatch>
        rowKey="id"
        loading={batches.isPending}
        dataSource={batches.data?.items ?? []}
        scroll={{ x: 720 }}
        locale={{ emptyText: batches.isError ? '批次載入失敗' : '尚無驗收批次' }}
        pagination={{ current: page, pageSize: 20, total: batches.data?.total ?? 0, showSizeChanger: false, onChange: setPage }}
        columns={[
          {
            title: '批次參考版本',
            dataIndex: 'versionLabel',
            render: (_, batch) => <Link to={`/projects/${String(projectId)}/qa/${String(batch.id)}`}>{batch.versionLabel}</Link>,
          },
          { title: '環境', dataIndex: 'environment' },
          { title: '項目', dataIndex: 'total' },
          { title: '通過', dataIndex: 'passed' },
          { title: '失敗', dataIndex: 'failed' },
          { title: '受阻', dataIndex: 'blocked' },
          { title: '未測', dataIndex: 'notTested' },
        ]}
      />
      <Modal
        title="建立驗收批次"
        open={open}
        onCancel={() => {
          setOpen(false);
        }}
        onOk={() => {
          form.submit();
        }}
        confirmLoading={create.isPending}
        okText="建立"
        cancelText="取消"
      >
        {create.error ? <Alert type="error" title={create.error.message} /> : null}
        <Form<QaBatchInput>
          form={form}
          layout="vertical"
          onFinish={(values) => {
            create.mutate(values);
          }}
        >
          <Form.Item label="批次參考版本" name="versionLabel" rules={[{ required: true, whitespace: true }, { max: 200 }]}>
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item label="環境" name="environment" rules={[{ required: true, whitespace: true }, { max: 1000 }]}>
            <Input.TextArea maxLength={1000} />
          </Form.Item>
        </Form>
      </Modal>
    </Flex>
  );
}
