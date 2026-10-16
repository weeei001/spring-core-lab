import type { GuideStep } from '@/shared/ui/PageTour';
import { TASKS_TOUR } from './components/tasks/tour';

/** 專案詳情的操作導覽；target 對應元件上的 data-tour 屬性 */
export const PROJECT_DETAIL_TOUR: GuideStep[] = [
  {
    target: 'project-header',
    title: '專案資訊',
    description: '編號、起訖日與整體進度。專案經理可以編輯資料或刪除專案（還有任務時無法刪除）。',
  },
  {
    target: 'project-status',
    title: '專案狀態',
    description: '規劃中 → 進行中 → 結案；也可以暫停或取消。結案、取消後專案變成唯讀，需要時再「重新開啟」。',
    placement: 'bottomLeft',
  },
  {
    target: 'project-tabs',
    title: '分頁',
    description: '概覽看進度與待辦；任務是主要工作區；Sprint 規劃迭代並看燃盡圖；里程碑是階段目標；標籤區分任務範圍；成員管理誰能參與；工時看大家投入多少時間。',
  },
  // 停在任務分頁時，重看教學也會帶到任務區的說明
  ...TASKS_TOUR,
];
