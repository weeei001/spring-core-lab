import { darkAlgorithm, darkTheme, defaultAlgorithm, defaultTheme } from '@ant-design/compatible';
import { theme, type ThemeConfig } from 'antd';
import type { ThemeMode } from './theme-mode';

/**
 * Ant Design V4 風格：主色拂曉藍 #1890ff、圓角 2、線框風格，選單選中時右側有藍色指示條。
 * 色彩演算法與 Menu token 來自官方的 @ant-design/compatible，用法與 antd 官網首頁的 V4 主題相同。
 * 版面與元件的共用外觀都集中在這裡設定，頁面不要再用 style 覆寫 antd 元件的底色、邊距與邊框
 */
export function getV4Theme(mode: ThemeMode): ThemeConfig {
  const base: ThemeConfig =
    mode === 'dark'
      ? { ...darkTheme, algorithm: [theme.darkAlgorithm, darkAlgorithm] }
      : { ...defaultTheme, algorithm: [theme.defaultAlgorithm, defaultAlgorithm] };
  // compatible 只換了主色，info 色（Progress、Alert、Tag 的 processing）會停在 v6 的 #1677ff
  const token = { ...base.token, colorInfo: '#1890ff' };
  // component token 只能給固定色值，頁首要跟著深淺色走，所以先用同一份設定算出容器色
  const { colorBgContainer, colorBgLayout } = theme.getDesignToken({ ...base, token });

  return {
    ...base,
    token,
    components: {
      ...base.components,
      Layout: {
        // 淺色用 V4 的內容區灰底；antd 預設頁首是深藍、左右留 50px
        bodyBg: mode === 'dark' ? colorBgLayout : '#f0f2f5',
        headerBg: colorBgContainer,
        headerPadding: '0 24px',
      },
      Menu: {
        ...base.components?.Menu,
        // 側欄選單不要右側框線，底色跟著側欄
        activeBarBorderWidth: 0,
        itemBg: 'transparent',
        subMenuItemBg: 'transparent',
      },
      // 標題都放在 Flex 裡用 gap 排間距，不吃 antd 預設的上下 margin
      Typography: { titleMarginTop: 0, titleMarginBottom: 0 },
    },
  };
}
