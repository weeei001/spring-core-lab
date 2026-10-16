import type { GuideStep } from '@/shared/ui/PageTour';

/** 工時表的操作導覽；target 對應元件上的 data-tour 屬性 */
export const TIMESHEET_TOUR: GuideStep[] = [
  {
    target: 'timesheet-week',
    title: '選擇週次',
    description: '左右箭頭切換上一週、下一週；「本週」回到今天。週一是一週的開始。',
    placement: 'bottomRight',
  },
  {
    target: 'timesheet-table',
    title: '直接填時數',
    description:
      '每列是一個任務（指派給你的未完成任務，加上這週登記過的），每欄是一天。輸入時數後離開格子就儲存，以 0.5 小時為單位；清空或填 0 就刪除。同一天有多筆紀錄的格子請到任務詳情修改。最下面一列是每日合計。',
    placement: 'top',
  },
];
