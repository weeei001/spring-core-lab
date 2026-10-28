import { useState } from 'react';
import { App, Button, Drawer, Dropdown, Flex, Grid, Layout, Menu, Typography, type MenuProps } from 'antd';
import { LogoutOutlined, MenuOutlined, MoonOutlined, QuestionCircleOutlined, SunOutlined, UserOutlined } from '@ant-design/icons';
import { Link, Outlet, useLocation, useNavigate } from 'react-router';
import { NotificationBell } from '@/components/notification/NotificationBell';
import { GlobalSearch } from '@/components/search/GlobalSearch';
import { useAuth } from '@/core/auth/useAuth';
import { openPageTour, resetPageTours } from '@/shared/ui/PageTour';
import { MENU } from '../menu';
import { useThemeMode } from '../theme-mode';
import './AppLayout.css';

/** 登入後的版面：左側選單（依權限顯示）、上方登入者與登出、中間是各頁面 */
export function AppLayout() {
  const { user, can, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, toggle } = useThemeMode();
  const { message } = App.useApp();
  const screens = Grid.useBreakpoint();
  const mobile = !screens.lg;
  const [menuOpen, setMenuOpen] = useState(false);
  // 子頁面（例如 /projects/12）也要亮起所屬的選單項目：取最長的前綴
  const selectedKey =
    MENU.flatMap((group) => group.pages.map((page) => page.path))
      .filter((path) => location.pathname === path || location.pathname.startsWith(`${path}/`))
      .sort((a, b) => b.length - a.length)[0] ?? location.pathname;

  const items: MenuProps['items'] = MENU.map((group) => ({
    label: group.label,
    pages: group.pages.filter((page) => can(page.permission)),
  }))
    .filter((group) => group.pages.length > 0)
    .map((group) => ({
      type: 'group' as const,
      key: group.label,
      label: group.label,
      children: group.pages.map((page) => ({ key: page.path, label: page.label })),
    }));

  const brand = (
    <Link to="/" className="app-brand" aria-label="案步就班，回到首頁" onClick={() => { setMenuOpen(false); }}>
      案步就班
    </Link>
  );
  const navigation = (
    <nav aria-label="主要導覽" data-tour="app-menu">
      <Menu
        mode="inline"
        items={items}
        selectedKeys={[selectedKey]}
        onClick={({ key }) => {
          setMenuOpen(false);
          void navigate(key);
        }}
      />
    </nav>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {mobile ? (
        <Drawer
          title={brand}
          placement="left"
          size="min(280px, 85vw)"
          open={menuOpen}
          onClose={() => { setMenuOpen(false); }}
          styles={{ body: { padding: 0 } }}
        >
          {navigation}
        </Drawer>
      ) : (
        <Layout.Sider width={200} theme="light" className="app-sidebar">
          {brand}
          {navigation}
        </Layout.Sider>
      )}
      <Layout style={{ minWidth: 0 }}>
        <Layout.Header className="app-header">
          <Flex justify="end" align="center" gap={mobile ? 4 : 12} style={{ height: '100%' }}>
            {mobile && (
              <>
                <Button
                  type="text"
                  icon={<MenuOutlined />}
                  aria-label="開啟導覽選單"
                  aria-expanded={menuOpen}
                  onClick={() => { setMenuOpen(true); }}
                />
                {brand}
              </>
            )}
            <div data-tour="global-search" style={{ marginInlineEnd: mobile ? 0 : 'auto' }}>
              <GlobalSearch compact={mobile} />
            </div>
            <NotificationBell />
            {mobile ? (
              <Dropdown trigger={['click']} menu={{ items: [
                { key: 'name', label: user.name, disabled: true },
                { key: 'theme', label: mode === 'dark' ? '切換為淺色' : '切換為深色', icon: mode === 'dark' ? <SunOutlined /> : <MoonOutlined /> },
                { key: 'page', label: '本頁教學', icon: <QuestionCircleOutlined /> },
                { key: 'reset', label: '重新顯示所有頁面的教學' },
                { type: 'divider' },
                { key: 'logout', label: '登出', icon: <LogoutOutlined /> },
              ], onClick: ({ key }) => {
                if (key === 'theme') toggle();
                if (key === 'logout') void logout();
                if (key === 'page') openPageTour();
                if (key === 'reset') {
                  resetPageTours();
                  void message.success('各頁面下次進入時會再顯示教學');
                  openPageTour();
                }
              } }}>
                <Button type="text" aria-label="帳號與設定" icon={<UserOutlined />} />
              </Dropdown>
            ) : (
              <>
                <Dropdown
                  trigger={['click']}
                  menu={{
                    items: [
                      { key: 'page', label: '本頁教學' },
                      { key: 'reset', label: '重新顯示所有頁面的教學' },
                    ],
                    onClick: ({ key }) => {
                      if (key === 'reset') {
                        resetPageTours();
                        void message.success('各頁面下次進入時會再顯示教學');
                      }
                      openPageTour();
                    },
                  }}
                >
                  <Button type="text" aria-label="使用教學" data-tour="help" icon={<QuestionCircleOutlined />} />
                </Dropdown>
                <Button
                  type="text"
                  data-tour="theme-toggle"
                  aria-label={mode === 'dark' ? '切換為淺色' : '切換為深色'}
                  icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
                  onClick={toggle}
                />
                <Typography.Text ellipsis style={{ maxWidth: 160 }} title={user.name}>{user.name}</Typography.Text>
                <Button
                  icon={<LogoutOutlined />}
                  onClick={() => {
                    void logout();
                  }}
                >
                  登出
                </Button>
                  </>
            )}
          </Flex>
        </Layout.Header>
        <Layout.Content className="app-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
