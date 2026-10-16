import type { GuideStep } from '@/shared/ui/PageTour';

/** 任務分頁的操作導覽；target 對應元件上的 data-tour 屬性 */
export const TASKS_TOUR: GuideStep[] = [
  {
    target: 'task-views',
    title: '三種檢視',
    description: '表格：可排序、直接改狀態與指派人。看板：依狀態分欄拖曳。甘特：依日期排時程、看前置任務箭頭。',
    placement: 'bottomRight',
  },
  {
    target: 'task-filters',
    title: '篩選',
    description: '關鍵字、指派人、優先度、類型、標籤、Sprint、里程碑；三種檢視共用同一組條件，切換檢視不會重設。',
  },
  {
    target: 'task-create',
    title: '新增任務',
    description: '填完整資料用這個按鈕；在看板每一欄底部輸入標題按 Enter，可以快速新增到那一欄。',
    placement: 'bottomRight',
  },
  {
    target: 'task-content',
    title: '拖曳更新進度',
    description:
      '看板：把卡片拖到別欄就是改狀態，同一欄上下拖是調整順序。甘特：拖曳長條改日期、拉長兩端改起訖。前置任務還沒完成就開始時，系統會先提醒你。點任務打開詳細資料。',
    placement: 'top',
  },
];
