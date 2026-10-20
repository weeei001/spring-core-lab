import type { LeaveStatus, LeaveType } from '@/api/leave/leave.types';

export const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  PERSONAL: '事假',
  SICK: '病假',
  ANNUAL: '特休',
  OTHER: '其他',
};

export const LEAVE_STATUS_LABELS: Record<LeaveStatus, string> = {
  PENDING: '待審核',
  APPROVED: '已核准',
  REJECTED: '已退回',
};

export const LEAVE_STATUS_COLORS: Record<LeaveStatus, string> = {
  PENDING: 'processing',
  APPROVED: 'success',
  REJECTED: 'error',
};
