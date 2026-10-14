import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, Flex, Result, Spin, message } from 'antd';
import { ApiError, LOGIN_URL } from '@/core/http/client';
import { authApi } from './auth-api';
import { AuthContext, type AuthContextValue } from './auth-context';

function FullPageSpin({ text }: { text: string }) {
  return (
    <Flex vertical justify="center" align="center" gap={16} style={{ height: '100vh' }}>
      <Spin size="large" />
      {text}
    </Flex>
  );
}

/**
 * 啟動時取得目前登入者；未登入時 http 層會整頁導向 authentik 登入。
 * 登入失敗時 web-server 會帶 ?loginError= 導回來，這時顯示錯誤，不再自動導向登入。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const loginError = new URLSearchParams(window.location.search).get('loginError');
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me, enabled: loginError === null });

  if (loginError !== null) {
    return (
      <Result
        status="warning"
        title="登入失敗"
        subTitle={loginError}
        extra={
          <Button type="primary" href={LOGIN_URL}>
            重新登入
          </Button>
        }
      />
    );
  }
  if (me.isPending) {
    return <FullPageSpin text="載入中…" />;
  }
  if (me.isError) {
    if (me.error instanceof ApiError && me.error.status === 401) {
      return <FullPageSpin text="導向登入頁…" />;
    }
    return (
      <Result
        status="error"
        title="無法取得登入資訊"
        subTitle={me.error.message}
        extra={
          <Button
            onClick={() => {
              void me.refetch();
            }}
          >
            重試
          </Button>
        }
      />
    );
  }

  const user = me.data;
  const value: AuthContextValue = {
    user,
    can: (permission) => user.permissions.includes(permission),
    logout: async () => {
      try {
        window.location.href = await authApi.logout();
      } catch (error: unknown) {
        void message.error(error instanceof Error ? error.message : '登出失敗');
      }
    },
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
