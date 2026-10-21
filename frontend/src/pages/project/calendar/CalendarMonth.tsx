import type { CSSProperties, MouseEvent } from 'react';
import { Button, Flex, Tooltip, Typography, theme } from 'antd';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { DATE_FORMAT, todayText } from '@/components/project/project-utils';
import { layoutWeek, monthWeeks, type CalendarEvent, type WeekSegment } from './calendar-events';
import './calendar.css';

/** 每週最多顯示幾列橫條，其餘收成「+N」 */
const MAX_LANES = 4;
const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];

interface CalendarMonthProps {
  value: Dayjs;
  events: CalendarEvent[];
  onSelect: (day: Dayjs) => void;
  onOpen: (event: CalendarEvent) => void;
}

function barClass(segment: WeekSegment): string {
  const { event } = segment;
  return [
    'team-calendar-bar',
    event.kind === '休假' ? 'is-leave' : event.overdue ? 'is-overdue' : 'is-task',
    event.only !== undefined && 'is-partial',
    segment.continuesBefore && 'is-continued-before',
    segment.continuesAfter && 'is-continued-after',
  ].filter(Boolean).join(' ');
}

/** 月曆：跨日的休假與任務畫成連續橫條，跨週時在每週各畫一段 */
export function CalendarMonth({ value, events, onSelect, onOpen }: CalendarMonthProps) {
  const { token } = theme.useToken();
  const today = todayText();
  const selected = value.format(DATE_FORMAT);
  const vars = {
    '--cal-text': token.colorText,
    '--cal-text-secondary': token.colorTextSecondary,
    '--cal-text-muted': token.colorTextQuaternary,
    '--cal-border': token.colorSplit,
    '--cal-radius': `${String(token.borderRadiusSM)}px`,
    '--cal-weekend': token.colorFillQuaternary,
    '--cal-hover': token.controlItemBgHover,
    '--cal-selected': token.controlItemBgActive,
    '--cal-primary': token.colorPrimary,
    '--cal-on-primary': token.colorTextLightSolid,
    '--cal-leave-bg': token.colorSuccessBg,
    '--cal-leave-border': token.colorSuccessBorder,
    '--cal-leave-accent': token.colorSuccess,
    '--cal-task-bg': token.colorPrimaryBg,
    '--cal-task-border': token.colorPrimaryBorder,
    '--cal-task-accent': token.colorPrimary,
    '--cal-overdue-bg': token.colorErrorBg,
    '--cal-overdue-border': token.colorErrorBorder,
    '--cal-overdue-accent': token.colorError,
  } as CSSProperties;

  // 休假沒有連結，點橫條就選取滑鼠所在的那一天
  const clickBar = (segment: WeekSegment, monday: Dayjs) => (e: MouseEvent<HTMLButtonElement>) => {
    if (segment.event.link !== undefined) {
      onOpen(segment.event);
      return;
    }
    const rect = e.currentTarget.getBoundingClientRect();
    const span = segment.endCol - segment.startCol + 1;
    const offset = e.detail === 0 ? 0 : Math.min(span - 1, Math.floor(((e.clientX - rect.left) / rect.width) * span));
    onSelect(monday.add(segment.startCol + offset, 'day'));
  };

  return (
    <Flex vertical gap={12} className="team-calendar" style={vars}>
      <Flex gap={8} align="center">
        <Button icon={<LeftOutlined />} aria-label="上個月" onClick={() => { onSelect(value.subtract(1, 'month')); }} />
        <Button onClick={() => { onSelect(dayjs()); }}>今天</Button>
        <Button icon={<RightOutlined />} aria-label="下個月" onClick={() => { onSelect(value.add(1, 'month')); }} />
        <Typography.Title level={5}>{value.format('YYYY 年 M 月')}</Typography.Title>
      </Flex>
      <div className="team-calendar-grid">
        <div className="team-calendar-weekdays">
          {WEEKDAYS.map((name, col) => (
            <div key={name} className={col >= 5 ? 'team-calendar-weekday is-weekend' : 'team-calendar-weekday'}>{name}</div>
          ))}
        </div>
        {monthWeeks(value).map((monday) => {
          const days = Array.from({ length: 7 }, (_, col) => monday.add(col, 'day'));
          const segments = layoutWeek(events, monday);
          const hidden = days.map((_, col) =>
            segments.filter((s) => s.lane >= MAX_LANES && s.startCol <= col && s.endCol >= col).length);
          return (
            <div key={monday.format(DATE_FORMAT)} className="team-calendar-week">
              <div className="team-calendar-days">
                {days.map((day, col) => {
                  const text = day.format(DATE_FORMAT);
                  return (
                    <button key={text} type="button" aria-label={day.format('M 月 D 日 dddd')} aria-pressed={text === selected}
                      className={['team-calendar-day', col >= 5 && 'is-weekend', text === selected && 'is-selected']
                        .filter(Boolean).join(' ')}
                      onClick={() => { onSelect(day); }} />
                  );
                })}
              </div>
              <div className="team-calendar-lanes">
                {days.map((day, col) => (
                  <div key={col} style={{ gridColumn: col + 1 }}
                    className={['team-calendar-date', !day.isSame(value, 'month') && 'is-other-month',
                      day.format(DATE_FORMAT) === today && 'is-today'].filter(Boolean).join(' ')}>
                    <span>{day.date() === 1 ? day.format('M/D') : day.date()}</span>
                  </div>
                ))}
                {segments.filter((s) => s.lane < MAX_LANES).map((segment) => (
                  <Tooltip key={segment.event.key} title={<>{segment.event.title}<br />{segment.event.period}</>}>
                    <button type="button" className={barClass(segment)}
                      style={{ gridRow: segment.lane + 2, gridColumn: `${String(segment.startCol + 1)} / ${String(segment.endCol + 2)}` }}
                      onClick={clickBar(segment, monday)}>
                      {segment.event.title}
                    </button>
                  </Tooltip>
                ))}
                {hidden.map((count, col) => count > 0 && (
                  <button key={col} type="button" className="team-calendar-more"
                    style={{ gridRow: MAX_LANES + 2, gridColumn: col + 1 }}
                    onClick={() => { onSelect(monday.add(col, 'day')); }}>
                    +{count} 筆
                  </button>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </Flex>
  );
}
