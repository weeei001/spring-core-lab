import { Flex, Typography } from 'antd';
import type { GuideStep } from '@/shared/ui/PageTour';

/** 儀表板的操作導覽；target 對應元件上的 data-tour 屬性 */
export const DASHBOARD_TOUR: GuideStep[] = [
  {
    title: '歡迎使用專案控管',
    description: (
      <Flex vertical gap={8}>
        <Typography.Text>一個專案通常這樣進行：</Typography.Text>
        <ol style={{ margin: 0, paddingInlineStart: 20 }}>
          <li>建立專案，你會自動成為專案經理</li>
          <li>加入一起工作的成員</li>
          <li>把工作拆成任務，指派給成員</li>
          <li>在看板拖曳卡片更新進度，有問題就到任務裡留言</li>
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
