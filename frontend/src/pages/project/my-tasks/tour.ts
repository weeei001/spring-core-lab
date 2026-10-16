import type { GuideStep } from '@/shared/ui/PageTour';

/** 我的任務的操作導覽；target 對應元件上的 data-tour 屬性 */
export const MY_TASKS_TOUR: GuideStep[] = [
  {
    target: 'my-tasks-list',
    title: '指派給你的任務',
    description: '跨所有專案，依逾期、今天、本週、之後分組。左邊可以直接改狀態，點標題打開任務詳情。',
    placement: 'top',
  },
  {
    target: 'my-tasks-finished',
    title: '已完成的任務',
    description: '預設只列未完成的，打開這裡可以回顧做完的工作。',
    placement: 'bottomRight',
  },
];
