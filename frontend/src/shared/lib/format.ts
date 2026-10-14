import dayjs from 'dayjs';

/** LocalDateTime 字串轉成「2026-10-14 10:48」 */
export function formatTime(value: string): string {
  return dayjs(value).format('YYYY-MM-DD HH:mm');
}

/** 空白字串轉成 undefined；後端的 keyword 不接受空字串 */
export function blankToUndefined(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed === undefined || trimmed === '' ? undefined : trimmed;
}

/** 代碼對照表轉成 Select 的 options */
export function toOptions<K extends string>(labels: Record<K, string>): { value: K; label: string }[] {
  return (Object.keys(labels) as K[]).map((value) => ({ value, label: labels[value] }));
}
