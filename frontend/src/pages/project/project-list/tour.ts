import type { GuideStep } from '@/shared/ui/PageTour';

/** 專案列表的操作導覽；target 對應元件上的 data-tour 屬性 */
export const PROJECT_LIST_TOUR: GuideStep[] = [
  {
    target: 'project-create',
    title: '建立專案',
    description: '填名稱、起訖日與顏色。建立的人自動成為專案經理，可以再邀請其他成員。',
    placement: 'bottomRight',
  },
  {
    target: 'project-filters',
    title: '篩選',
    description: '預設只列出還在進行的專案；勾掉狀態可以看已結案、取消的專案。打開「我是經理」只看你負責的。',
  },
  {
    target: 'project-view',
    title: '卡片或表格',
    description: '卡片適合快速瀏覽，表格適合比較進度與逾期數。選擇會被記住。',
    placement: 'bottomRight',
  },
  {
    target: 'project-cards',
    title: '專案卡片',
    description: '頂部色條是專案顏色；進度條是完成的主任務比例；紅色數字是逾期任務；右下角是你在專案裡的角色。點卡片進入專案。',
    placement: 'top',
  },
];
