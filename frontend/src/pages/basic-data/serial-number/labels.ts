import type { ResetCycle } from '@/api/basic-data/serial-number.types';

export const RESET_CYCLE_LABELS: Record<ResetCycle, string> = {
  NONE: '不歸零',
  YEAR: '每年',
  MONTH: '每月',
  DAY: '每日',
};
