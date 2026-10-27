import type { GuideStep } from '@/shared/ui/PageTour';

/** 任務分頁的操作導覽；target 對應元件上的 data-tour 屬性 */
export const TASKS_TOUR: GuideStep[] = [
  {
    target: 'task-filters',
    title: '篩選',
    description: '用編號或標題找任務，也可以選「指派給我」只看自己的工作。',
  },
  {
    target: 'task-create',
    title: '新增任務',
    description: '先填標題，需要時再補說明、負責人與日期。看板每欄底部也能輸入標題按 Enter 快速新增。',
    placement: 'bottomRight',
  },
  {
    target: 'task-content',
    title: '查看與更新任務',
    description:
      '可切換看板或表格，兩者共用篩選。看板可拖曳卡片，表格可排序及展開子任務；點標題開啟詳情。編輯操作依專案權限開放。',
    placement: 'top',
  },
];
