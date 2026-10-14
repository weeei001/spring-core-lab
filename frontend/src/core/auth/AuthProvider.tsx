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

/** 未登入時的首頁；按登入才整頁跳轉到 authentik。error 是 web-server 帶回來的登入失敗原因 */
function LoginPage({ error }: { error: string | null }) {
  return (
    <Result
      status={error === null ? 'info' : 'warning'}
      title={error === null ? '請先登入' : '登入失敗'}
      subTitle={error}
      extra={
        <Button type="primary" href={LOGIN_URL}>
          {error === null ? '登入' : '重新登入'}
        </Button>
      }
    />
  );
}

/**
 * 啟動時取得目前登入者；未登入（含登出後 authentik 導回來）時顯示登入頁，由使用者自己按登入。
 * 登入失敗時 web-server 會帶 ?loginError= 導回來，這時顯示錯誤原因。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const loginError = new URLSearchParams(window.location.search).get('loginError');
  const me = useQuery({ queryKey: ['me'], queryFn: authApi.me, enabled: loginError === null });

  if (loginError !== null) {
    return <LoginPage error={loginError} />;
  }
  if (me.isPending) {
    return <FullPageSpin text="載入中…" />;
  }
  if (me.isError) {
    if (me.error instanceof ApiError && me.error.status === 401) {
      return <LoginPage error={null} />;
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
