import type { BaseEntity } from '@/core/http/types';

export type LeaveType = 'PERSONAL' | 'SICK' | 'ANNUAL' | 'OTHER';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN' | 'CANCELLED';
export type LeaveScope = 'mine' | 'review';

export interface LeaveRequest extends BaseEntity {
  applicantId: string;
  approverId: string;
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
  status: LeaveStatus;
  reviewComment: string | null;
  reviewedAt: string | null;
  cancelReason: string | null;
  cancelledAt: string | null;
  version: number;
}

export interface LeaveSubmission {
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
  approverId: string;
}

export interface LeaveReview {
  decision: 'APPROVED' | 'REJECTED';
  comment?: string;
}

export interface LeaveQuery {
  status?: LeaveStatus;
}

export interface LeaveCancellation {
  reason: string;
}

/** 團隊行事曆只取得休假人員與日期。 */
export interface LeaveAbsence {
  applicantId: string;
  startDate: string;
  endDate: string;
}
