import { http, send } from '@/core/http/client';
import type { PageResponse, Result } from '@/core/http/types';
import type { QaBatch, QaBatchInput, QaBugExecution, QaExecution, QaExecutionInput, QaItem, QaItemInput } from './manual-qa.types';

export const manualQaApi = {
  batches: (projectId: number, page: number) => send(http.post<Result<PageResponse<QaBatch>>>('/api/manual-qa-batch/queryPage', { projectId, page, size: 20 })),
  batch: (id: number) => send(http.get<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}`)),
  create: (projectId: number, body: QaBatchInput) => send(http.post<Result<QaBatch>>('/api/manual-qa-batch', { projectId, ...body })),
  updateBatch: (id: number, body: QaBatchInput) => send(http.patch<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}`, body)),
  copy: (id: number, body: QaBatchInput) => send(http.post<Result<QaBatch>>(`/api/manual-qa-batch/${String(id)}/copy`, body)),
  items: (id: number, page: number) => send(http.get<Result<PageResponse<QaItem>>>(`/api/manual-qa-batch/${String(id)}/items`, { params: { page, size: 100 } })),
  createItem: (id: number, body: QaItemInput) => send(http.post<Result<QaItem>>(`/api/manual-qa-batch/${String(id)}/items`, body)),
  updateItem: (id: number, body: Partial<QaItemInput>) => send(http.patch<Result<QaItem>>(`/api/manual-qa-item/${String(id)}`, body)),
  deleteItem: (id: number) => send(http.delete<Result<void>>(`/api/manual-qa-item/${String(id)}`)),
  executions: (id: number, page: number) => send(http.get<Result<PageResponse<QaExecution>>>(`/api/manual-qa-item/${String(id)}/executions`, { params: { page, size: 20 } })),
  execute: (id: number, body: QaExecutionInput) => send(http.post<Result<QaExecution>>(`/api/manual-qa-item/${String(id)}/executions`, body)),
  updateBug: (id: number, bugTaskId: number | null) => send(http.patch<Result<QaExecution>>(`/api/manual-qa-execution/${String(id)}/bug`, { bugTaskId })),
  bugExecutions: (id: number, page: number) => send(http.get<Result<PageResponse<QaBugExecution>>>(`/api/manual-qa-bug/${String(id)}/executions`, { params: { page, size: 20 } })),
};
