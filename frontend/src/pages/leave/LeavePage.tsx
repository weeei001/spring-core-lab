import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { App, Button, DatePicker, Descriptions, Drawer, Flex, Form, Input, Radio, Result, Select, Skeleton, Table, Tag, Timeline, Typography, type TableColumnsType } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { useSearchParams } from 'react-router';
import { leaveApi } from '@/api/leave/leave-api';
import type { LeaveCancellation, LeaveQuery, LeaveRequest, LeaveReview, LeaveScope, LeaveStatus, LeaveType } from '@/api/leave/leave.types';
import { UserSelect } from '@/components/user/UserSelect';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import { PERMISSIONS } from '@/core/auth/permissions';
import { useAuth } from '@/core/auth/useAuth';
import { usePagedList } from '@/shared/hooks/usePagedList';
import { blankToUndefined, formatTime, toOptions } from '@/shared/lib/format';
import { FormModal } from '@/shared/ui/FormModal';
import { LEAVE_STATUS_COLORS, LEAVE_STATUS_LABELS, LEAVE_TYPE_LABELS } from './labels';

interface LeaveFormValues {
  type: LeaveType;
  dates: [Dayjs, Dayjs];
  reason: string;
  approverId: string;
}

/** 兩個入口共用列表與詳情；後端分開限制申請人、審核人範圍。 */
export function LeavePage({ scope }: { scope: LeaveScope }) {
  const reviewing = scope === 'review';
  const { user } = useAuth();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [status, setStatus] = useState<LeaveStatus | undefined>(reviewing ? 'PENDING' : undefined);
  const [params, setParams] = useSearchParams();
  const requestId = Number(params.get('request'));
  const selectedId = Number.isSafeInteger(requestId) && requestId > 0 ? requestId : undefined;
  const list = usePagedList<LeaveRequest, LeaveQuery>(
    `leave-${scope}`,
    (query) => leaveApi.queryPage(scope, query),
    reviewing ? { status: 'PENDING' } : {},
  );
  const directory = useUserDirectory(list.rows.flatMap((row) => [row.applicantId, row.approverId]));

  const refresh = () => queryClient.invalidateQueries({
    predicate: (query) => String(query.queryKey[0]).startsWith('leave-'),
  });
  const openDetail = (id: number | undefined): void => {
    setParams((current) => {
      const next = new URLSearchParams(current);
      if (id === undefined) {
        next.delete('request');
      } else {
        next.set('request', String(id));
      }
      return next;
    }, { replace: true });
  };
  const submit = useMutation({
    mutationFn: leaveApi.submit,
    onSuccess: async (request) => {
      setCreating(false);
      openDetail(request.id);
      void message.success('已送出請假申請');
      await refresh();
    },
  });

  const columns: TableColumnsType<LeaveRequest> = [
    { title: '單號', dataIndex: 'id', width: 80, render: (id: number) => `#${String(id)}` },
    {
      title: reviewing ? '申請人' : '審核人',
      key: 'person',
      width: 150,
      render: (_value, row) => directory.name(reviewing ? row.applicantId : row.approverId),
    },
    { title: '假別', key: 'type', width: 90, render: (_value, row) => LEAVE_TYPE_LABELS[row.type] },
    { title: '日期', key: 'dates', render: (_value, row) => `${row.startDate} ～ ${row.endDate}` },
    { title: '狀態', key: 'status', width: 110, render: (_value, row) => <LeaveStatusTag status={row.status} /> },
    { title: '送出時間', key: 'createTime', width: 160, render: (_value, row) => formatTime(row.createTime) },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_value, row) => (
        <Button type="link" size="small" onClick={() => { openDetail(row.id); }}>
          {reviewing && row.status === 'PENDING' ? '審核' : '查看'}
        </Button>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Typography.Title level={4}>{reviewing ? '請假審核' : '我的請假'}</Typography.Title>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Flex gap={8}>
          <Select<LeaveStatus>
            aria-label="申請狀態"
            placeholder="全部狀態"
            allowClear
            value={status}
            options={toOptions(LEAVE_STATUS_LABELS)}
            style={{ width: 160 }}
            onChange={(value: LeaveStatus | undefined) => {
              setStatus(value);
              list.search(value === undefined ? {} : { status: value });
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={() => { void refresh(); }}>重新整理</Button>
        </Flex>
        {!reviewing && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreating(true); }}>申請請假</Button>
        )}
      </Flex>
      <Table<LeaveRequest>
        rowKey="id"
        columns={columns}
        dataSource={list.rows}
        loading={list.loading}
        pagination={list.pagination}
        scroll={{ x: 850 }}
        locale={{ emptyText: reviewing ? '沒有符合條件的審核單' : '尚無請假申請' }}
      />
      <FormModal<LeaveFormValues>
        open={creating}
        title="申請請假"
        okText="送出申請"
        initialValues={{ type: 'PERSONAL' }}
        saving={submit.isPending}
        onCancel={() => { if (!submit.isPending) setCreating(false); }}
        onSubmit={(values) => {
          if (!submit.isPending) {
            submit.mutate({
              type: values.type,
              startDate: values.dates[0].format('YYYY-MM-DD'),
              endDate: values.dates[1].format('YYYY-MM-DD'),
              reason: values.reason.trim(),
              approverId: values.approverId,
            });
          }
        }}
      >
        <Form.Item name="type" label="假別" rules={[{ required: true, message: '請選擇假別' }]}>
          <Select options={toOptions(LEAVE_TYPE_LABELS)} />
        </Form.Item>
        <Form.Item name="dates" label="請假日期" extra="以整天申請，包含開始與結束日期。" rules={[{ required: true, message: '請選擇日期區間' }]}>
          <DatePicker.RangePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="approverId" label="審核人" extra="只顯示可審核請假的人員，不能選擇自己。" rules={[{ required: true, message: '請選擇審核人' }]}>
          <UserSelect excludeIds={[String(user.id)]} permissionCode={PERMISSIONS.LEAVE_APPROVE} placeholder="輸入審核人的姓名或帳號" />
        </Form.Item>
        <Form.Item name="reason" label="請假原因" rules={[{ required: true, whitespace: true, message: '請填寫請假原因' }, { max: 1000 }]}>
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
      </FormModal>
      <Drawer
        title={selectedId === undefined ? '請假申請' : `請假申請 #${String(selectedId)}`}
        open={selectedId !== undefined}
        onClose={() => { openDetail(undefined); }}
        size={640}
        destroyOnHidden
      >
        {selectedId !== undefined && (
          <LeaveDetail key={`${scope}-${String(selectedId)}`} scope={scope} id={selectedId} refresh={refresh} />
        )}
      </Drawer>
    </Flex>
  );
}

function LeaveStatusTag({ status }: { status: LeaveStatus }) {
  return <Tag color={LEAVE_STATUS_COLORS[status]}>{LEAVE_STATUS_LABELS[status]}</Tag>;
}

function LeaveDetail({ scope, id, refresh }: { scope: LeaveScope; id: number; refresh: () => Promise<void> }) {
  const { can } = useAuth();
  const { message } = App.useApp();
  const [form] = Form.useForm<LeaveReview>();
  const [cancellationAction, setCancellationAction] = useState<'withdraw' | 'cancel'>();
  const detail = useQuery({
    queryKey: ['leave-detail', scope, id],
    queryFn: () => leaveApi.detail(scope, id),
    staleTime: 0,
    refetchOnWindowFocus: 'always',
  });
  const directory = useUserDirectory([detail.data?.applicantId, detail.data?.approverId]);
  const review = useMutation({
    mutationFn: (values: LeaveReview) => leaveApi.review(id, values),
    onSuccess: (saved) => {
      void message.success(saved.status === 'APPROVED' ? '已核准請假' : '已退回申請');
      form.resetFields();
    },
    // 包含另一個視窗已審核的 409，重新讀取才能收回過期的審核按鈕。
    onSettled: refresh,
  });
  const cancel = useMutation({
    mutationFn: (values: LeaveCancellation) => cancellationAction === 'withdraw'
      ? leaveApi.withdraw(id, values)
      : leaveApi.cancel(id, values),
    onSuccess: (saved) => {
      setCancellationAction(undefined);
      void message.success(saved.status === 'WITHDRAWN' ? '已撤回請假申請' : '已銷假');
    },
    onSettled: refresh,
  });
  if (detail.isPending) {
    return <Skeleton active />;
  }
  if (detail.isError) {
    return <Result status="warning" title="無法讀取申請" subTitle={detail.error.message} />;
  }
  const request = detail.data;
  const canReview = scope === 'review' && can(PERMISSIONS.LEAVE_APPROVE) && request.status === 'PENDING';
  const canCancel = scope === 'mine' && can(PERMISSIONS.LEAVE_APPLY)
    && (request.status === 'PENDING' || request.status === 'APPROVED');
  const cancelLabel = request.status === 'PENDING' ? '撤回申請' : '整筆銷假';

  return (
    <Flex vertical gap={24}>
      <Descriptions column={1} bordered size="small" items={[
        { key: 'status', label: '狀態', children: <LeaveStatusTag status={request.status} /> },
        { key: 'applicant', label: '申請人', children: directory.name(request.applicantId) },
        { key: 'approver', label: '審核人', children: directory.name(request.approverId) },
        { key: 'type', label: '假別', children: LEAVE_TYPE_LABELS[request.type] },
        { key: 'dates', label: '請假日期', children: `${request.startDate} ～ ${request.endDate}` },
        { key: 'reason', label: '請假原因', children: <span style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{request.reason}</span> },
      ]} />
      <div>
        <Typography.Title level={5}>操作紀錄</Typography.Title>
        <Timeline items={[
          {
            content: `${formatTime(request.createTime)} · ${directory.name(request.applicantId)} 送出申請`,
          },
          ...(request.reviewedAt !== null ? [{
            color: request.status === 'REJECTED' ? 'red' : 'green',
            content: (
              <Flex vertical gap={4}>
                <span>{formatTime(request.reviewedAt)} · {directory.name(request.approverId)} {request.status === 'REJECTED' ? '已退回' : '已核准'}</span>
                {request.reviewComment !== null && <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', margin: 0 }}>{request.reviewComment}</Typography.Paragraph>}
              </Flex>
            ),
          }] : request.status === 'PENDING' ? [{
            color: 'gray',
            content: `等待 ${directory.name(request.approverId)} 審核`,
          }] : []),
          ...(request.cancelledAt !== null ? [{
            color: 'gray',
            content: (
              <Flex vertical gap={4}>
                <span>{formatTime(request.cancelledAt)} · {directory.name(request.applicantId)} {LEAVE_STATUS_LABELS[request.status]}</span>
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', margin: 0 }}>{request.cancelReason}</Typography.Paragraph>
              </Flex>
            ),
          }] : []),
        ]} />
      </div>
      {canCancel && (
        <>
          <Button danger onClick={() => { setCancellationAction(request.status === 'PENDING' ? 'withdraw' : 'cancel'); }}>{cancelLabel}</Button>
          <FormModal<LeaveCancellation>
            open={cancellationAction !== undefined}
            title={cancelLabel}
            okText={`確認${cancelLabel}`}
            saving={cancel.isPending}
            onCancel={() => { if (!cancel.isPending) setCancellationAction(undefined); }}
            onSubmit={(values) => { if (!cancel.isPending) cancel.mutate({ reason: values.reason.trim() }); }}
          >
            <Typography.Paragraph>
              {request.status === 'PENDING' ? '撤回後審核人無法再核准這筆申請。' : '銷假會取消這筆申請的全部休假日期，原核准紀錄仍會保留。'}
              如需更改日期，請另送新申請。
            </Typography.Paragraph>
            <Form.Item name="reason" label="原因" rules={[{ required: true, whitespace: true, message: '請填寫原因' }, { max: 1000 }]}>
              <Input.TextArea rows={3} maxLength={1000} showCount />
            </Form.Item>
          </FormModal>
        </>
      )}
      {canReview && (
        <Form<LeaveReview>
          form={form}
          layout="vertical"
          initialValues={{ decision: 'APPROVED' }}
          onFinish={(values) => {
            if (!review.isPending) {
              review.mutate({ decision: values.decision, comment: blankToUndefined(values.comment) });
            }
          }}
        >
          <Form.Item name="decision" label="審核結果" rules={[{ required: true }]}>
            <Radio.Group options={[
              { value: 'APPROVED', label: '核准' },
              { value: 'REJECTED', label: '退回' },
            ]} />
          </Form.Item>
          <Form.Item name="comment" label="審核意見" dependencies={['decision']} rules={[
            { max: 1000 },
            ({ getFieldValue }) => ({
              validator: (_rule, value: string | undefined) => getFieldValue('decision') === 'REJECTED' && !value?.trim()
                ? Promise.reject(new Error('退回時請填寫原因'))
                : Promise.resolve(),
            }),
          ]}>
            <Input.TextArea rows={3} maxLength={1000} showCount placeholder="退回時必填" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={review.isPending}>送出審核</Button>
        </Form>
      )}
    </Flex>
  );
}
