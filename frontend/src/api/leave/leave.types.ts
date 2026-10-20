import type { BaseEntity } from '@/core/http/types';

export type LeaveType = 'PERSONAL' | 'SICK' | 'ANNUAL' | 'OTHER';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
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
  decision: Exclude<LeaveStatus, 'PENDING'>;
  comment?: string;
}

export interface LeaveQuery {
  status?: LeaveStatus;
}
