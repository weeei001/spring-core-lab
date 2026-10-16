import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { Tour, type TourProps } from 'antd';

/** 導覽的一步；target 對應畫面上 data-tour="..." 的元素，不給則置中顯示（例如歡迎頁） */
export interface GuideStep {
  target?: string;
  title: string;
  description: ReactNode;
  placement?: TourProps['placement'];
}

const OPEN_EVENT = 'page-tour:open';
const SEEN_PREFIX = 'tour-seen:';
/** 自動播放前等一下，讓資料載入、進場動畫跑完，位置才準 */
const AUTO_DELAY = 800;
/** 蓋過 Drawer、Modal */
const Z_INDEX = 1100;

/** 同一時間只播一個導覽（例如專案頁與任務分頁同時第一次出現） */
let running: string | null = null;
/** 畫面上可重播的導覽，後掛載的在上面（例如任務抽屜蓋在專案頁上）；「?」只重播最上面那個 */
const replayStack: string[] = [];

function hasSeen(id: string): boolean {
  try {
    return localStorage.getItem(SEEN_PREFIX + id) !== null;
  } catch {
    // 無法存取 localStorage 時當作看過，避免每次都跳出來
    return true;
  }
}

function markSeen(id: string): void {
  try {
    localStorage.setItem(SEEN_PREFIX + id, '1');
  } catch {
    // 存不了就算了
  }
}

/** 重播目前頁面的導覽（上方列的「?」按鈕） */
export function openPageTour(): void {
  window.dispatchEvent(new Event(OPEN_EVENT));
}

/** 清掉所有「看過」紀錄，各頁下次進入時會再自動播放 */
export function resetPageTours(): void {
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith(SEEN_PREFIX))
      .forEach((key) => {
        localStorage.removeItem(key);
      });
  } catch {
    // 存不了就算了
  }
}

/** 找 data-tour="key" 的元素；有 data-tour-inner（CSS 選擇器）時改指向它裡面的那個子元素，例如只框分頁列 */
function findTarget(key: string): HTMLElement | null {
  const element = document.querySelector<HTMLElement>(`[data-tour="${key}"]`);
  const inner = element?.dataset['tourInner'];
  return inner === undefined ? element : (element?.querySelector<HTMLElement>(inner) ?? element);
}

interface PageTourProps {
  /** 每頁唯一，記錄是否看過 */
  id: string;
  steps: readonly GuideStep[];
  /** false：只在第一次自動播放，不回應「?」按鈕（例如分頁裡的導覽，已併入所在頁面的導覽） */
  replayable?: boolean;
}

/**
 * 頁面導覽：第一次進入自動播放，之後可從上方列的「?」重播。
 * 播放時只保留畫面上找得到目標的步驟（例如沒有權限而沒顯示的按鈕就略過）。
 */
export function PageTour({ id, steps, replayable = true }: PageTourProps) {
  const [open, setOpen] = useState(false);
  const [current, setCurrent] = useState(0);
  const [visible, setVisible] = useState<NonNullable<TourProps['steps']>>([]);

  const start = useCallback((): boolean => {
    if (running !== null && running !== id) {
      return false;
    }
    const resolved = steps.flatMap((step) => {
      if (step.target === undefined) {
        return [{ title: step.title, description: step.description, placement: step.placement }];
      }
      const target = findTarget(step.target);
      // 找不到或沒有顯示（沒有權限、空的容器）就略過這一步
      return target === null || target.getClientRects().length === 0
        ? []
        : [{ title: step.title, description: step.description, placement: step.placement, target }];
    });
    if (resolved.length === 0) {
      return false;
    }
    running = id;
    setVisible(resolved);
    setCurrent(0);
    setOpen(true);
    return true;
  }, [id, steps]);

  const finish = (): void => {
    running = null;
    setOpen(false);
    markSeen(id);
  };

  useEffect(() => {
    if (hasSeen(id)) {
      return undefined;
    }
    const timer = window.setTimeout(() => {
      start();
    }, AUTO_DELAY);
    return () => {
      window.clearTimeout(timer);
    };
  }, [id, start]);

  useEffect(() => {
    if (!replayable) {
      return undefined;
    }
    replayStack.push(id);
    const onOpen = (): void => {
      if (replayStack.at(-1) === id) {
        start();
      }
    };
    window.addEventListener(OPEN_EVENT, onOpen);
    return () => {
      window.removeEventListener(OPEN_EVENT, onOpen);
      const index = replayStack.lastIndexOf(id);
      if (index >= 0) {
        replayStack.splice(index, 1);
      }
    };
  }, [id, replayable, start]);

  // 離開頁面時若還在播，釋放佔用
  useEffect(
    () => () => {
      if (running === id) {
        running = null;
      }
    },
    [id],
  );

  return (
    <Tour
      open={open}
      current={current}
      steps={visible}
      type="primary"
      zIndex={Z_INDEX}
      onChange={setCurrent}
      onClose={finish}
      onFinish={finish}
    />
  );
}
