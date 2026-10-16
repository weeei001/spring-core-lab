import { createContext, useContext } from 'react';

export type ThemeMode = 'light' | 'dark';

export interface ThemeModeValue {
  mode: ThemeMode;
  toggle: () => void;
}

export const ThemeModeContext = createContext<ThemeModeValue | null>(null);

const KEY = 'theme-mode';

/** 上次選的模式；沒選過時跟隨作業系統；瀏覽器不允許存取 localStorage 時用淺色 */
export function loadThemeMode(): ThemeMode {
  try {
    const saved = localStorage.getItem(KEY);
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

export function saveThemeMode(mode: ThemeMode): void {
  try {
    localStorage.setItem(KEY, mode);
  } catch {
    // 存不了就下次跟隨作業系統
  }
}

export function useThemeMode(): ThemeModeValue {
  const value = useContext(ThemeModeContext);
  if (!value) {
    throw new Error('useThemeMode 必須在 App 內使用');
  }
  return value;
}
