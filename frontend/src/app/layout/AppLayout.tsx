import { App, Button, Dropdown, Flex, Layout, Menu, Typography, theme, type MenuProps } from 'antd';
import { LogoutOutlined, MoonOutlined, QuestionCircleOutlined, SunOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { NotificationBell } from '@/components/notification/NotificationBell';
import { GlobalSearch } from '@/components/search/GlobalSearch';
import { useAuth } from '@/core/auth/useAuth';
import { openPageTour, resetPageTours } from '@/shared/ui/PageTour';
import { MENU } from '../menu';
import { useThemeMode } from '../theme-mode';

/** 登入後的版面：左側選單（依權限顯示）、上方登入者與登出、中間是各頁面 */
export function AppLayout() {
  const { user, can, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const { mode, toggle } = useThemeMode();
  const { message } = App.useApp();
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

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider width={200} theme="light" breakpoint="lg" collapsedWidth={0}>
        <Typography.Title level={5} style={{ padding: '16px 24px', margin: 0 }}>
          spring-core-lab
        </Typography.Title>
        <div data-tour="app-menu">
          <Menu
            mode="inline"
            items={items}
            selectedKeys={[selectedKey]}
            onClick={({ key }) => {
              void navigate(key);
            }}
            style={{ borderInlineEnd: 0 }}
          />
        </div>
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: token.colorBgContainer, paddingInline: 24 }}>
          <Flex justify="end" align="center" gap={12} style={{ height: '100%' }}>
            <div data-tour="global-search" style={{ marginInlineEnd: 'auto' }}>
              <GlobalSearch />
            </div>
            <NotificationBell />
            <Dropdown
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
            <Typography.Text>{user.name}</Typography.Text>
            <Button
              icon={<LogoutOutlined />}
              onClick={() => {
                void logout();
              }}
            >
              登出
            </Button>
          </Flex>
        </Layout.Header>
        <Layout.Content style={{ margin: 24 }}>
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
