import type { BaseEntity } from '@/core/http/types';

export type ResetCycle = 'NONE' | 'YEAR' | 'MONTH' | 'DAY';

/** 系統編號規則（後端 SerialNumber） */
export interface SerialNumber extends BaseEntity {
  /** 規則代碼，例如 PROJECT；建立後不可修改 */
  serialKey: string;
  name: string;
  /** 編號格式，例如 P{yyyyMM}{0000} */
  rule: string;
  /** 最後使用的流水號，由取號維護 */
  lastIndex: number;
  resetCycle: ResetCycle;
}

export interface SerialNumberQuery {
  keyword?: string;
}
