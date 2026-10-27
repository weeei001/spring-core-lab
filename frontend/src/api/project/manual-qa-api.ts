import { http, send } from '@/core/http/client';
import type { PageResponse, Result } from '@/core/http/types';
import type { QaBatch, QaBatchInput, QaBugExecution, QaExecution, QaExecutionInput, QaItem, QaItemInput } from './manual-qa.types';

export const manualQaApi = {
  batches: (projectId: number, page: number) =>
    send(http.post<Result<PageResponse<QaBatch>>>('/api/manual-qa-batch/queryPage', { projectId, page, size: 20 })),
  batch: (id: number) => send(http.get<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}`)),
  create: (projectId: number, body: QaBatchInput) =>
    send(http.post<Result<QaBatch>>('/api/manual-qa-batch', { projectId, ...body })),
  updateBatch: (id: number, body: QaBatchInput) =>
    send(http.patch<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}`, body)),
  copy: (id: number, body: QaBatchInput) => send(http.post<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}/copy`, body)),
  items: (batchId: number, page: number) =>
    send(http.post<Result<PageResponse<QaItem>>>('/api/manual-qa-item/queryPage', { batchId, page, size: 100 })),
  createItem: (batchId: number, body: QaItemInput) =>
    send(http.post<Result<QaItem>>('/api/manual-qa-item', { batchId, ...body })),
  /** null 代表清空：改送 clearFields，其他欄位照原值送出。 */
  updateItem: (id: number, body: Partial<QaItemInput>) => {
    const clearFields = (['taskId', 'assigneeId'] as const).filter((field) => body[field] === null);
    const values = Object.fromEntries(Object.entries(body).filter(([, value]) => value !== null));
    return send(http.patch<Result<QaItem>>(
      `/api/manual-qa-item/${String(id)}`,
      clearFields.length === 0 ? values : { ...values, clearFields },
    ));
  },
  deleteItem: (id: number) => send(http.delete<Result<void>>(`/api/manual-qa-item/${String(id)}`)),
  executions: (itemId: number, page: number) =>
    send(http.post<Result<PageResponse<QaExecution>>>('/api/manual-qa-execution/queryPage', { itemId, page, size: 20 })),
  execute: (itemId: number, body: QaExecutionInput) =>
    send(http.post<Result<QaExecution>>('/api/manual-qa-execution', { itemId, ...body })),
  updateBug: (id: number, bugTaskId: number | null) =>
    send(http.patch<Result<QaExecution>>(
      `/api/manual-qa-execution/${String(id)}`,
      bugTaskId === null ? { clearFields: ['bugTaskId'] } : { bugTaskId },
    )),
  bugExecutions: (taskId: number, page: number) =>
    send(http.get<Result<PageResponse<QaBugExecution>>>(`/api/manual-qa-execution/bug/${String(taskId)}`, { params: { page, size: 20 } })),
};
