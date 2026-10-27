import type { Location } from 'react-router';

/** 來源來自網址，僅接受同源的絕對站內路徑。 */
export function taskSourcePath(value: string | null): string | undefined {
  if (value === null || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')
    || Array.from(value).some((character) => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127)) {
    return undefined;
  }
  const url = new URL(value, window.location.origin);
  return url.origin === window.location.origin && !url.pathname.startsWith('//')
    ? `${url.pathname}${url.search}${url.hash}`
    : undefined;
}

export function taskDetailPath(taskId: number, from?: string): string {
  return `/tasks/${String(taskId)}${from === undefined ? '' : `?${new URLSearchParams({ from }).toString()}`}`;
}

/** 清單與舊抽屜共用捲動位置；返回來源的新 history entry 也能還原。 */
export function taskListScrollKey(location: Pick<Location, 'pathname' | 'search' | 'key'>): string {
  if (!/^\/projects\/\d+$/.test(location.pathname)) {
    return location.key;
  }
  const params = new URLSearchParams(location.search);
  params.delete('task');
  params.sort();
  return `${location.pathname}?${params.toString()}`;
}
