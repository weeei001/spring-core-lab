/**
 * 功能權限代碼，與後端 Permission enum 相同；新增權限時兩邊一起改。
 * 前端只用來隱藏選單和按鈕，真正擋住請求的是後端的 @PreAuthorize。
 */
export const PERMISSIONS = {
  PRODUCT_READ: 'product:read',
  PRODUCT_WRITE: 'product:write',
  PRODUCT_STOCK: 'product:stock',
  BRAND_WRITE: 'brand:write',
  TAG_WRITE: 'tag:write',
  SERIAL_NUMBER_READ: 'serial-number:read',
  SERIAL_NUMBER_WRITE: 'serial-number:write',
  ROLE_MANAGE: 'role:manage',
} as const;

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];
