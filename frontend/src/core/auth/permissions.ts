/**
 * 功能權限代碼，與後端 Permission enum 相同；新增權限時兩邊一起改。
 * 前端只用來隱藏選單和按鈕，真正擋住請求的是後端的 @PreAuthorize。
 */
export const PERMISSIONS = {
  PROJECT_READ: 'project:read',
  PROJECT_WRITE: 'project:write',
  PROJECT_ADMIN: 'project:admin',
  TASK_WRITE: 'task:write',
  MEETING_WRITE: 'meeting:write',
  WORK_LOG_WRITE: 'work-log:write',
  LEAVE_APPLY: 'leave:apply',
  LEAVE_APPROVE: 'leave:approve',
  SERIAL_NUMBER_READ: 'serial-number:read',
  SERIAL_NUMBER_WRITE: 'serial-number:write',
  ROLE_MANAGE: 'role:manage',
  NOTIFICATION_SEND: 'notification:send',
} as const;

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];
