import { Button, Flex, Layout, Menu, Typography, theme, type MenuProps } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '@/core/auth/useAuth';
import { MENU } from '../menu';

/** 登入後的版面：左側選單（依權限顯示）、上方登入者與登出、中間是各頁面 */
export function AppLayout() {
  const { user, can, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();

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
        <Menu
          mode="inline"
          items={items}
          selectedKeys={[location.pathname]}
          onClick={({ key }) => {
            void navigate(key);
          }}
          style={{ borderInlineEnd: 0 }}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header style={{ background: token.colorBgContainer, paddingInline: 24 }}>
          <Flex justify="end" align="center" gap={12} style={{ height: '100%' }}>
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
