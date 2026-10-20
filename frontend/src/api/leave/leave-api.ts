import { http, send } from '@/core/http/client';
import type { PageQuery, PageResponse, Result } from '@/core/http/types';
import type { LeaveQuery, LeaveRequest, LeaveReview, LeaveScope, LeaveSubmission } from './leave.types';

const BASE = '/api/leave-request';

export const leaveApi = {
  queryPage: (scope: LeaveScope, query: LeaveQuery & PageQuery) =>
    send(http.post<Result<PageResponse<LeaveRequest>>>(`${BASE}/${scope}/queryPage`, query)),
  detail: (scope: LeaveScope, id: number) =>
    send(http.get<Result<LeaveRequest>>(`${BASE}/${scope}/${String(id)}`)),
  submit: (values: LeaveSubmission) => send(http.post<Result<LeaveRequest>>(BASE, values)),
  review: (id: number, values: LeaveReview) =>
    send(http.post<Result<LeaveRequest>>(`${BASE}/${String(id)}/review`, values)),
};
