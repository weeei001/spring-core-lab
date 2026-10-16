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
    description: '狀態、指派人、日期等欄位改完立即儲存，不用按儲存鍵。按欄位的 ✕ 就是清空。',
    placement: 'left',
  },
  {
    target: 'drawer-predecessors',
    title: '前置任務',
    description: '設定「哪些任務完成後才能開始這個」。前置任務會在甘特圖畫成箭頭，系統也會擋下循環相依。',
    placement: 'left',
  },
  {
    target: 'drawer-tabs',
    title: '子任務、留言與工時',
    description: '把大任務拆成子任務勾選完成；在留言與活動看誰改了什麼、和同事討論；在工時登記你花在這個任務的時間。',
    placement: 'top',
  },
];
