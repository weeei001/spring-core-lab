import type { GuideStep } from '@/shared/ui/PageTour';

/** 任務抽屜的操作導覽；target 對應元件上的 data-tour 屬性 */
export const TASK_DRAWER_TOUR: GuideStep[] = [
  {
    target: 'drawer-title',
    title: '任務詳情',
    description: '點標題旁的筆可以直接改標題；上方的專案標籤可以回到專案。網址會帶著任務編號，複製連結給同事就能打開同一個任務。',
    placement: 'bottom',
  },
  {
    target: 'drawer-fields',
    title: '改了就存',
    description: '狀態、負責人與日期改完立即儲存，不用按儲存鍵。按欄位的 ✕ 就是清空。',
    placement: 'left',
  },
  {
    target: 'drawer-more',
    title: '需要時再補設定',
    description: '展開「更多設定」可調整分類、優先度、預估工時與前置任務。日常更新進度只要使用上方欄位。',
    placement: 'left',
  },
  {
    target: 'drawer-tabs',
    title: '留言討論',
    description: '預設開啟「留言與活動」，可和同事討論並查看修改紀錄。需要時再切換到子任務、附件或工時。',
    placement: 'top',
  },
];
