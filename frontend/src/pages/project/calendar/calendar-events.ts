import dayjs, { type Dayjs } from 'dayjs';
import type { LeaveAbsence } from '@/api/leave/leave.types';
import type { Task } from '@/api/project/task.types';
import { DATE_FORMAT, isOverdue, weekStart } from '@/components/project/project-utils';

export type CalendarKind = '休假' | '任務';

/** 休假排在任務上方，誰不在一眼就看得到 */
const KIND_ORDER: Record<CalendarKind, number> = { 休假: 0, 任務: 1 };

/** 行事曆上的一個事項；start、end 為 YYYY-MM-DD，含頭尾 */
export interface CalendarEvent {
  key: string;
  kind: CalendarKind;
  title: string;
  start: string;
  end: string;
  /** 起訖說明，顯示在提示與當日清單 */
  period: string;
  /** 任務只設定了開始日或到期日其中一個 */
  only?: '僅開始' | '僅到期';
  overdue: boolean;
  link?: string;
}

/** 事項在某一週的一段：col 為 0（週一）～ 6（週日），lane 為由上往下第幾列 */
export interface WeekSegment {
  event: CalendarEvent;
  startCol: number;
  endCol: number;
  lane: number;
  /** 從上一週延續過來、會延續到下一週 */
  continuesBefore: boolean;
  continuesAfter: boolean;
}

/** 月曆顯示的週（週一開始），包含前後月份補滿的日期 */
export function monthWeeks(month: Dayjs): Dayjs[] {
  const last = month.endOf('month');
  const weeks: Dayjs[] = [];
  for (let week = weekStart(month.startOf('month')); !week.isAfter(last, 'day'); week = week.add(1, 'week')) {
    weeks.push(week);
  }
  return weeks;
}

export function taskEvent(task: Task): CalendarEvent | undefined {
  const start = task.startDate ?? task.dueDate;
  const end = task.dueDate ?? task.startDate;
  if (start === null || end === null) return undefined;

  let period: string;
  if (task.startDate === null) {
    period = `${end} 到期 · 未設定開始日`;
  } else if (task.dueDate === null) {
    period = `${start} 開始 · 未設定到期日`;
  } else {
    period = start === end ? `${start} 當日任務` : `${start} ～ ${end}`;
  }
  return {
    key: `task-${String(task.id)}`,
    kind: '任務',
    title: `${task.code} ${task.title}`,
    // 開始日晚於到期日的資料仍顯示在開始日，不讓橫條消失
    start,
    end: end < start ? start : end,
    period,
    only: task.startDate === null ? '僅到期' : task.dueDate === null ? '僅開始' : undefined,
    overdue: isOverdue(task),
    link: `/projects/${String(task.projectId)}?tab=tasks&task=${String(task.id)}`,
  };
}

/** 同一人重疊或相連的休假合成一段，月曆上才會是一條連續的橫條 */
export function leaveEvents(leaves: LeaveAbsence[], name: (id: string) => string): CalendarEvent[] {
  const merged: LeaveAbsence[] = [];
  const sorted = [...leaves].sort((a, b) =>
    a.applicantId.localeCompare(b.applicantId) || a.startDate.localeCompare(b.startDate));
  for (const leave of sorted) {
    const last = merged.at(-1);
    const nextDay = last === undefined ? undefined : dayjs(last.endDate).add(1, 'day').format(DATE_FORMAT);
    if (last !== undefined && nextDay !== undefined && last.applicantId === leave.applicantId && leave.startDate <= nextDay) {
      if (leave.endDate > last.endDate) last.endDate = leave.endDate;
    } else {
      merged.push({ ...leave });
    }
  }
  return merged.map((leave) => ({
    key: `leave-${leave.applicantId}-${leave.startDate}`,
    kind: '休假',
    title: `${name(leave.applicantId)} 休假`,
    start: leave.startDate,
    end: leave.endDate,
    period: leave.startDate === leave.endDate ? `${leave.startDate} 休假` : `${leave.startDate} ～ ${leave.endDate}`,
    overdue: false,
  }));
}

/** 當日在事項期間中的位置，用於當日清單 */
export function dayLabel(event: CalendarEvent, day: string): string {
  if (event.kind === '休假') return '休假';
  if (event.only !== undefined) return event.only;
  if (event.start === event.end) return '當日';
  return day === event.start ? '開始' : day === event.end ? '到期' : '期間';
}

/**
 * 把事項切成一週一段，排到不重疊的列：休假在上，其次依開始日、長的先排，
 * 跨日橫條才會穩定在上方。
 */
export function layoutWeek(events: CalendarEvent[], monday: Dayjs): WeekSegment[] {
  const from = monday.format(DATE_FORMAT);
  const to = monday.add(6, 'day').format(DATE_FORMAT);
  const col = (date: string): number => dayjs(date).diff(monday, 'day');
  const segments = events
    .filter((event) => event.start <= to && event.end >= from)
    .map((event) => ({
      event,
      startCol: event.start < from ? 0 : col(event.start),
      endCol: event.end > to ? 6 : col(event.end),
      lane: 0,
      continuesBefore: event.start < from,
      continuesAfter: event.end > to,
    }))
    .sort((a, b) => KIND_ORDER[a.event.kind] - KIND_ORDER[b.event.kind] || a.startCol - b.startCol
      || (b.endCol - b.startCol) - (a.endCol - a.startCol) || a.event.title.localeCompare(b.event.title));

  const lanes: boolean[][] = [];
  for (const segment of segments) {
    let used = lanes.find((lane) => lane.slice(segment.startCol, segment.endCol + 1).every((taken) => !taken));
    if (used === undefined) {
      used = Array.from({ length: 7 }, () => false);
      lanes.push(used);
    }
    used.fill(true, segment.startCol, segment.endCol + 1);
    segment.lane = lanes.indexOf(used);
  }
  return segments;
}
