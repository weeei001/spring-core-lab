import { useEffect, useRef } from 'react';
import { animate } from 'motion/react';

interface CountUpProps {
  value: number;
  /** 小數位數 */
  decimals?: number;
}

/** 數字從 0 跑到 value 的動畫；value 改變時從目前的值接著跑 */
export function CountUp({ value, decimals = 0 }: CountUpProps) {
  const ref = useRef<HTMLSpanElement>(null);
  const current = useRef(0);

  useEffect(() => {
    const node = ref.current;
    if (node === null) {
      return undefined;
    }
    const controls = animate(current.current, value, {
      duration: 0.8,
      ease: 'easeOut',
      onUpdate: (latest) => {
        current.current = latest;
        node.textContent = latest.toFixed(decimals);
      },
    });
    return () => {
      controls.stop();
    };
  }, [value, decimals]);

  return <span ref={ref}>{(0).toFixed(decimals)}</span>;
}
