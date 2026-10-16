import { useEffect, useState } from 'react';

/** 值停止變動 delay 毫秒後才更新，搜尋框用來避免每打一個字就送一次請求 */
export function useDebounced<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebounced(value);
    }, delay);
    return () => {
      window.clearTimeout(timer);
    };
  }, [value, delay]);
  return debounced;
}
