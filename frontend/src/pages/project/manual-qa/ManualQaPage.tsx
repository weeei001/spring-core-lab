import { useCallback, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Breadcrumb, Button, Card, Descriptions, Flex, Form, Input, InputNumber, List, Modal, Pagination, Popconfirm, Result, Select, Skeleton, Space, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { Link, useBeforeUnload, useBlocker, useNavigate, useParams } from 'react-router';
import { manualQaApi } from '@/api/project/manual-qa-api';
import type { QaBatchInput, QaExecutionInput, QaItem, QaItemInput, QaStatus } from '@/api/project/manual-qa.types';
import { taskApi } from '@/api/project/task-api';
import { useProjectContext } from '@/components/project/hooks';
import { TASK_TYPE_LABELS } from '@/components/project/labels';
import { useMemberDirectory } from '@/components/project/useMemberDirectory';
import { useUserDirectory } from '@/components/user/useUserDirectory';
import './manual-qa.css';

const STATUS: Record<QaStatus, string> = { NOT_TESTED: '未測', PASS: '通過', FAIL: '失敗', BLOCKED: '受阻' };
const COLORS: Record<QaStatus, string> = { NOT_TESTED: 'default', PASS: 'success', FAIL: 'error', BLOCKED: 'warning' };

export function ManualQaPage() {
  const params = useParams();
  const projectId = Number(params['id']);
  const batchId = Number(params['batchId']);
  if (!Number.isSafeInteger(projectId) || projectId <= 0 || !Number.isSafeInteger(batchId) || batchId <= 0) {
    return <Result status="404" title="找不到驗收批次" />;
  }
  return <ManualQaDetail key={`${String(projectId)}:${String(batchId)}`} projectId={projectId} batchId={batchId} />;
}

function ManualQaDetail({ projectId, batchId }: { projectId: number; batchId: number }) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const client = useQueryClient();
  const context = useProjectContext(projectId);
  const memberDirectory = useMemberDirectory(context.members);
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<number>();
  const [dirty, setDirty] = useState(false);
  const [batchAction, setBatchAction] = useState<'edit' | 'copy'>();
  const [itemAction, setItemAction] = useState<'new' | 'edit'>();
  const [batchForm] = Form.useForm<QaBatchInput>();
  const [itemForm] = Form.useForm<QaItemInput>();
  const batch = useQuery({ queryKey: ['manual-qa', 'batch', batchId], queryFn: () => manualQaApi.batch(batchId) });
  const items = useQuery({ queryKey: ['manual-qa', 'items', batchId, page], queryFn: () => manualQaApi.items(batchId, page) });
  const selected = items.data?.items.find((item) => item.id === selectedId);
  const blocker = useBlocker(dirty);
  useBeforeUnload(useCallback((event) => { if (dirty) { event.preventDefault(); } }, [dirty]));
  const refresh = () => Promise.all([
    client.invalidateQueries({ queryKey: ['manual-qa', 'batch', batchId] }),
    client.invalidateQueries({ queryKey: ['manual-qa', 'items', batchId] }),
    client.invalidateQueries({ queryKey: ['manual-qa', 'batches', projectId] }),
  ]);
  const saveBatch = useMutation({
    mutationFn: (values: QaBatchInput) => batchAction === 'copy' ? manualQaApi.copy(batchId, values) : manualQaApi.updateBatch(batchId, values),
    onSuccess: (saved) => {
      void refresh();
      setBatchAction(undefined);
      if (batchAction === 'copy') { void navigate(`/projects/${String(projectId)}/qa/${String(saved.id)}`); }
      else { void message.success('已更新批次'); }
    },
  });
  const saveItem = useMutation({
    mutationFn: (values: QaItemInput) => itemAction === 'edit' && selected ? manualQaApi.updateItem(selected.id, selected.latestExecution ? { assigneeId: values.assigneeId } : values) : manualQaApi.createItem(batchId, values),
    onSuccess: (saved) => {
      void refresh();
      if (itemAction === 'new') { setPage(Math.floor((items.data?.total ?? 0) / 100) + 1); }
      setItemAction(undefined);
      setSelectedId(saved.id);
      void message.success('已儲存項目');
    },
  });
  const remove = useMutation({ mutationFn: (id: number) => manualQaApi.deleteItem(id), onSuccess: () => { setSelectedId(undefined); void refresh(); } });
  const error = context.error ?? batch.error ?? items.error;
  if (error && (!context.project || !batch.data || !items.data)) {
    return <Result status="error" title={error.message} extra={<Button onClick={() => { context.retry(); void batch.refetch(); void items.refetch(); }}>重試</Button>} />;
  }
  if (context.loading || batch.isPending || items.isPending) { return <Skeleton active paragraph={{ rows: 10 }} />; }
  if (!context.project || !batch.data || batch.data.projectId !== projectId || !items.data) { return <Result status="404" title="找不到驗收批次或所屬專案" />; }
  const record = batch.data;
  const list = items.data;
  const canEdit = context.access.canEditTasks;
  const openBatch = (action: 'edit' | 'copy') => {
    batchForm.setFieldsValue(action === 'edit' ? { versionLabel: record.versionLabel, environment: record.environment } : { versionLabel: '', environment: '' });
    setBatchAction(action);
  };
  const openItem = (action: 'new' | 'edit') => {
    itemForm.setFieldsValue(action === 'edit' && selected ? selected : { title: '', steps: '', expectedResult: '', taskId: null, assigneeId: null });
    setItemAction(action);
  };
  return (
    <Flex vertical gap={16} style={{ minWidth: 0 }}>
      <Breadcrumb items={[{ title: <Link to="/projects">專案</Link> }, { title: <Link to={`/projects/${String(projectId)}?tab=qa`}>{context.project.name}</Link> }, { title: record.versionLabel }]} />
      {error ? <Alert type="error" title={`重新載入失敗，畫面可能不是最新資料：${error.message}`} action={<Button onClick={() => { context.retry(); void batch.refetch(); void items.refetch(); }}>重試</Button>} /> : null}
      <Card>
        <Flex justify="space-between" gap={16} wrap>
          <div><Typography.Title level={3}>{record.versionLabel}</Typography.Title><Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>{record.environment}</Typography.Paragraph>
            <Typography.Text>共 {record.total} 項 · 通過 {record.passed} · 失敗 {record.failed} · 受阻 {record.blocked} · 未測 {record.notTested} · {record.passRate === null ? '尚無項目' : `通過率 ${String(Math.round(record.passRate * 100))}%`}</Typography.Text>
          </div>
          {canEdit ? <Space wrap><Button disabled={dirty} onClick={() => { openBatch('edit'); }}>修改批次</Button><Button disabled={dirty} onClick={() => { openBatch('copy'); }}>複製新批次</Button><Button type="primary" disabled={dirty} onClick={() => { openItem('new'); }}>新增項目</Button></Space> : null}
        </Flex>
        {context.access.closed ? <Alert type="info" title="專案已結案或取消，驗收紀錄為唯讀" /> : null}
      </Card>
      <div className={`qa-layout${selected ? ' qa-selected' : ''}`}>
        <Card className="qa-list" title="檢查項目">
          <List dataSource={list.items} locale={{ emptyText: '尚無項目' }} renderItem={(item) => (
            <List.Item><Button type={selectedId === item.id ? 'primary' : 'text'} disabled={dirty && selectedId !== item.id} block style={{ height: 'auto', whiteSpace: 'normal', textAlign: 'left' }} onClick={() => { setSelectedId(item.id); }}><Tag color={COLORS[item.status]}>{STATUS[item.status]}</Tag>{item.title}</Button></List.Item>
          )} />
          {list.total > 100 ? <Pagination current={page} pageSize={100} total={list.total} disabled={dirty} showSizeChanger={false} onChange={(next) => { setPage(next); setSelectedId(undefined); }} /> : null}
        </Card>
        <Card className="qa-detail">
          {selected ? <>
            <Button className="qa-back" disabled={dirty} onClick={() => { setSelectedId(undefined); }}>返回項目清單</Button>
            <Flex justify="space-between" gap={12} wrap><Typography.Title level={4}>{selected.title} <Tag color={COLORS[selected.status]}>{STATUS[selected.status]}</Tag></Typography.Title>
              {canEdit ? <Space><Button disabled={dirty} onClick={() => { openItem('edit'); }}>編輯</Button>{selected.latestExecution === null ? <Popconfirm title="刪除尚未執行的項目？" onConfirm={() => { remove.mutate(selected.id); }}><Button danger disabled={dirty}>刪除</Button></Popconfirm> : null}</Space> : null}
            </Flex>
            {remove.error ? <Alert type="error" title={remove.error.message} /> : null}
            <Descriptions column={1} items={[
              { key: 'steps', label: '步驟', children: <span className="qa-text">{selected.steps}</span> },
              { key: 'expected', label: '預期結果', children: <span className="qa-text">{selected.expectedResult}</span> },
              { key: 'task', label: '關聯任務', children: selected.taskId === null ? '無' : <TaskReference id={selected.taskId} /> },
              { key: 'assignee', label: '負責人', children: selected.assigneeId === null ? '未指定' : memberDirectory.directory.name(selected.assigneeId) },
            ]} />
            {selected.latestExecution ? <Alert type="info" title="此項目已執行，標題、步驟、預期結果與關聯任務已鎖定。" /> : null}
            <ExecutionPanel key={selected.id} item={selected} canEdit={canEdit} dirty={dirty} setDirty={setDirty} onSaved={(next) => {
              void refresh();
              if (next) {
                const index = list.items.findIndex((item) => item.id === selected.id);
                const following = list.items[index + 1];
                if (following) { setSelectedId(following.id); }
                else if (page * 100 < list.total) {
                  const nextPage = page + 1;
                  void manualQaApi.items(batchId, nextPage)
                    .then((nextItems) => { client.setQueryData(['manual-qa', 'items', batchId, nextPage], nextItems); setPage(nextPage); setSelectedId(nextItems.items[0]?.id); })
                    .catch(() => { void message.error('無法載入下一頁項目，請重試'); });
                }
              }
            }} />
          </> : <Typography.Text type="secondary">選擇項目以查看內容與紀錄</Typography.Text>}
        </Card>
      </div>
      <Modal title={batchAction === 'copy' ? '複製新批次' : '修改批次'} open={batchAction !== undefined} onCancel={() => { setBatchAction(undefined); }} onOk={() => { batchForm.submit(); }} confirmLoading={saveBatch.isPending} okText="儲存" cancelText="取消">
        {batchAction === 'copy' ? <Typography.Paragraph>只複製項目，不複製執行結果。</Typography.Paragraph> : null}
        {saveBatch.error ? <Alert type="error" title={saveBatch.error.message} /> : null}
        <Form<QaBatchInput> form={batchForm} layout="vertical" onFinish={(values) => { saveBatch.mutate(values); }}>
          <Form.Item name="versionLabel" label="批次參考版本" rules={[{ required: true, whitespace: true }, { max: 200 }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="environment" label="環境" rules={[{ required: true, whitespace: true }, { max: 1000 }]}><Input.TextArea maxLength={1000} /></Form.Item>
        </Form>
      </Modal>
      <Modal title={itemAction === 'new' ? '新增項目' : '編輯項目'} open={itemAction !== undefined} onCancel={() => { setItemAction(undefined); }} onOk={() => { itemForm.submit(); }} confirmLoading={saveItem.isPending} okText="儲存" cancelText="取消">
        {saveItem.error ? <Alert type="error" title={saveItem.error.message} /> : null}
        <Form<QaItemInput> form={itemForm} layout="vertical" onFinish={(values) => { saveItem.mutate({ ...values, taskId: values.taskId ?? null, assigneeId: values.assigneeId || null }); }}>
          <Form.Item name="title" label="標題" rules={[{ required: true, whitespace: true }, { max: 200 }]}><Input maxLength={200} disabled={Boolean(itemAction === 'edit' && selected?.latestExecution)} /></Form.Item>
          <Form.Item name="steps" label="步驟" rules={[{ required: true, whitespace: true }]}><Input.TextArea rows={4} disabled={Boolean(itemAction === 'edit' && selected?.latestExecution)} /></Form.Item>
          <Form.Item name="expectedResult" label="預期結果" rules={[{ required: true, whitespace: true }]}><Input.TextArea rows={3} disabled={Boolean(itemAction === 'edit' && selected?.latestExecution)} /></Form.Item>
          <Form.Item name="taskId" label="關聯任務 ID（選填）"><InputNumber min={1} precision={0} style={{ width: '100%' }} disabled={Boolean(itemAction === 'edit' && selected?.latestExecution)} /></Form.Item>
          <Form.Item name="assigneeId" label="負責人（選填）"><Select allowClear showSearch={{ optionFilterProp: 'label' }} options={memberDirectory.options} /></Form.Item>
        </Form>
      </Modal>
      <Modal open={blocker.state === 'blocked'} title="放棄未儲存的結果？" okText="放棄並離開" cancelText="繼續編輯" onOk={() => { if (blocker.state === 'blocked') { blocker.proceed(); } }} onCancel={() => { if (blocker.state === 'blocked') { blocker.reset(); } }}>離開後會失去尚未儲存的內容。</Modal>
    </Flex>
  );
}

function ExecutionPanel({ item, canEdit, dirty, setDirty, onSaved }: { item: QaItem; canEdit: boolean; dirty: boolean; setDirty: (dirty: boolean) => void; onSaved: (next: boolean) => void }) {
  const { message } = App.useApp();
  const client = useQueryClient();
  const [page, setPage] = useState(1);
  const [form] = Form.useForm<QaExecutionInput>();
  const [bugForm] = Form.useForm<{ bugTaskId: number | null }>();
  const [bugExecution, setBugExecution] = useState<number>();
  const history = useQuery({ queryKey: ['manual-qa', 'executions', item.id, page], queryFn: () => manualQaApi.executions(item.id, page) });
  const directory = useUserDirectory((history.data?.items ?? []).map((execution) => execution.testerId));
  const result = Form.useWatch('result', form);
  const save = useMutation({
    mutationFn: ({ values }: { values: QaExecutionInput; next: boolean }) => manualQaApi.execute(item.id, { ...values, bugTaskId: values.result === 'FAIL' ? values.bugTaskId ?? null : null }),
    onSuccess: (_, variables) => {
      form.resetFields();
      setDirty(false);
      setPage(1);
      void client.invalidateQueries({ queryKey: ['manual-qa', 'executions', item.id] });
      onSaved(variables.next);
      void message.success('已儲存結果');
    },
  });
  const updateBug = useMutation({
    mutationFn: (value: number | null) => manualQaApi.updateBug(bugExecution ?? 0, value),
    onSuccess: () => { setBugExecution(undefined); void client.invalidateQueries({ queryKey: ['manual-qa', 'executions', item.id] }); onSaved(false); },
  });
  return (
    <Flex vertical gap={16} style={{ marginTop: 24 }}>
      {canEdit ? <Card title="記錄結果" size="small">
        {save.error ? <Alert type="error" title={save.error.message} style={{ marginBottom: 12 }} /> : null}
        <Form<QaExecutionInput> form={form} layout="vertical" disabled={save.isPending} onValuesChange={() => { setDirty(true); }} onFinish={(values) => { save.mutate({ values, next: false }); }}>
          <Form.Item name="testedVersion" label="實測版本" rules={[{ required: true, whitespace: true }, { max: 200 }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="result" label="結果" rules={[{ required: true }]}><Select options={[{ value: 'PASS', label: '通過' }, { value: 'FAIL', label: '失敗' }, { value: 'BLOCKED', label: '受阻' }]} /></Form.Item>
          <Form.Item name="actual" label="實際現象" rules={[{ required: true, whitespace: true, message: '請填寫實際現象或受阻原因' }]}><Input.TextArea rows={4} /></Form.Item>
          {result === 'FAIL' ? <Form.Item name="bugTaskId" label="BUG 任務 ID（選填）"><InputNumber min={1} precision={0} style={{ width: '100%' }} /></Form.Item> : null}
          <Space wrap>
            <Button type="primary" htmlType="submit" loading={save.isPending}>儲存結果</Button>
            <Button loading={save.isPending} onClick={() => { void form.validateFields().then((values) => { save.mutate({ values, next: true }); }).catch(() => undefined); }}>儲存並前往下一項</Button>
            {dirty ? <Popconfirm title="放棄尚未儲存的結果？" onConfirm={() => { form.resetFields(); setDirty(false); }}><Button>放棄輸入</Button></Popconfirm> : null}
          </Space>
        </Form>
      </Card> : null}
      <Card title="歷次紀錄" size="small">
        {history.error ? <Alert type="error" title={history.error.message} action={<Button onClick={() => { void history.refetch(); }}>重試</Button>} /> : null}
        <List loading={history.isPending} dataSource={history.data?.items ?? []} locale={{ emptyText: history.isError ? '紀錄載入失敗' : '尚無執行紀錄' }} renderItem={(execution) => (
          <List.Item><Flex vertical gap={4} style={{ width: '100%', minWidth: 0 }}>
            <Space wrap><Tag color={COLORS[execution.result]}>{STATUS[execution.result]}</Tag><Typography.Text>{execution.testedVersion}</Typography.Text><Typography.Text>{directory.name(execution.testerId)}</Typography.Text><Typography.Text>{dayjs(execution.executedAt).format('YYYY-MM-DD HH:mm:ss')}</Typography.Text></Space>
            <Typography.Text className="qa-text">{execution.actual}</Typography.Text>
            {execution.result === 'FAIL' ? <Space wrap><Typography.Text>BUG：{execution.bugTaskId === null ? '未連結' : <TaskReference id={execution.bugTaskId} />}</Typography.Text>{canEdit ? <Button size="small" disabled={dirty} onClick={() => { bugForm.setFieldsValue({ bugTaskId: execution.bugTaskId }); setBugExecution(execution.id); }}>修改連結</Button> : null}</Space> : null}
          </Flex></List.Item>
        )} />
        {(history.data?.total ?? 0) > 20 ? <Pagination current={page} pageSize={20} total={history.data?.total} showSizeChanger={false} onChange={setPage} /> : null}
      </Card>
      <Modal title="修改 BUG 連結" open={bugExecution !== undefined} onCancel={() => { setBugExecution(undefined); }} onOk={() => { bugForm.submit(); }} confirmLoading={updateBug.isPending} okText="儲存" cancelText="取消">
        {updateBug.error ? <Alert type="error" title={updateBug.error.message} /> : null}
        <Form<{ bugTaskId: number | null }> form={bugForm} layout="vertical" onFinish={(values) => { updateBug.mutate(values.bugTaskId ?? null); }}><Form.Item name="bugTaskId" label="BUG 任務 ID，留空解除連結"><InputNumber min={1} precision={0} style={{ width: '100%' }} /></Form.Item></Form>
      </Modal>
    </Flex>
  );
}

function TaskReference({ id }: { id: number }) {
  const task = useQuery({ queryKey: ['manual-qa', 'task', id], queryFn: () => taskApi.query({ ids: [id] }) });
  if (task.isPending) { return <>#{id} 載入中</>; }
  if (task.isError) { return <>#{id} 無法取得</>; }
  const found = task.data[0];
  return found ? <Link to={`/tasks/${String(id)}`}>#{id} {found.title}（{TASK_TYPE_LABELS[found.type]}）</Link> : <>#{id} 已刪除或無法取得</>;
}
