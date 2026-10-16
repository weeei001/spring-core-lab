import { Flex, Typography } from 'antd';
import type { GuideStep } from '@/shared/ui/PageTour';

/** 各頁面的操作導覽；target 對應元件上的 data-tour 屬性 */

export const DASHBOARD_TOUR: GuideStep[] = [
  {
    title: '歡迎使用專案控管',
    description: (
      <Flex vertical gap={8}>
        <Typography.Text>一個專案通常這樣進行：</Typography.Text>
        <ol style={{ margin: 0, paddingInlineStart: 20 }}>
          <li>建立專案，你會自動成為專案經理</li>
          <li>加入成員、設定里程碑</li>
          <li>把工作拆成任務，指派給成員</li>
          <li>在看板拖曳卡片更新進度，用甘特圖排時程</li>
          <li>每天在工時表登記花了多少時間</li>
        </ol>
        <Typography.Text type="secondary">接下來帶你認識畫面，約 1 分鐘。</Typography.Text>
      </Flex>
    ),
  },
  {
    target: 'app-menu',
    title: '主要功能',
    description: '儀表板看整體狀況；專案是所有工作的起點；我的任務列出跨專案指派給你的工作；工時用來登記每天的投入。',
    placement: 'right',
  },
  {
    target: 'dashboard-stats',
    title: '今天要注意的數字',
    description: '待辦、逾期、本週工時與參與中的專案。卡片可以點，直接跳到對應的頁面。',
  },
  {
    target: 'dashboard-due',
    title: '一週內到期',
    description: '點任務標題會在右側打開任務抽屜，不用離開這一頁就能改狀態、留言、登記工時。',
  },
  {
    target: 'dashboard-hours',
    title: '本週工時',
    description: '每天登記了幾小時；今天以深色標示。滑過長條可以看數字。',
  },
  {
    target: 'global-search',
    title: '全站搜尋',
    description: (
      <>
        在任何頁面按 <Typography.Text keyboard>Ctrl</Typography.Text> + <Typography.Text keyboard>K</Typography.Text>
        ，輸入專案名稱、任務標題或編號，用上下鍵選、Enter 開啟。
      </>
    ),
    placement: 'bottom',
  },
  {
    target: 'theme-toggle',
    title: '深色模式',
    description: '切換深淺色，選擇會記在這台電腦上。',
    placement: 'bottomRight',
  },
  {
    target: 'help',
    title: '隨時重看教學',
    description: '每一頁第一次進入都會有導覽；之後按這裡可以重看目前頁面的教學。',
    placement: 'bottomRight',
  },
];

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
    description: '關鍵字、指派人、優先度、里程碑；三種檢視共用同一組條件，切換檢視不會重設。',
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
    description: '概覽看進度與待辦；任務是主要工作區；里程碑是階段目標；成員管理誰能參與；工時看大家投入多少時間。',
  },
  // 停在任務分頁時，重看教學也會帶到任務區的說明
  ...TASKS_TOUR,
];

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
