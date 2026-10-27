import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { Modal } from 'antd';
import { useBeforeUnload, useBlocker } from 'react-router';

const DraftContext = createContext<(name: string, dirty: boolean) => void>(() => undefined);

/** 每個任務只有一個離開確認，包含未顯示的分頁；切換任務時由 key 清除草稿。 */
export function TaskDraftBoundary({ children }: { children: ReactNode }) {
  const [drafts, setDrafts] = useState<ReadonlySet<string>>(new Set());
  const setDirty = useCallback((name: string, dirty: boolean) => {
    setDrafts((current) => {
      if (current.has(name) === dirty) {
        return current;
      }
      const next = new Set(current);
      if (dirty) {
        next.add(name);
      } else {
        next.delete(name);
      }
      return next;
    });
  }, []);
  const dirty = drafts.size > 0;
  const blocker = useBlocker(({ currentLocation, nextLocation }) => dirty && (
    currentLocation.pathname !== nextLocation.pathname
    || new URLSearchParams(currentLocation.search).get('task') !== new URLSearchParams(nextLocation.search).get('task')
  ));
  useBeforeUnload(useCallback((event) => {
    if (dirty) {
      event.preventDefault();
    }
  }, [dirty]));

  return (
    <DraftContext value={setDirty}>
      {children}
      <Modal
        open={blocker.state === 'blocked'}
        title="放棄未送出的內容？"
        okText="放棄並離開"
        cancelText="繼續編輯"
        onOk={() => {
          if (blocker.state === 'blocked') {
            blocker.proceed();
          }
        }}
        onCancel={() => {
          if (blocker.state === 'blocked') {
            blocker.reset();
          }
        }}
      >
        留言或編輯內容尚未儲存，離開後會失去這些內容。
      </Modal>
    </DraftContext>
  );
}

export function useTaskDraft(name: string, dirty: boolean): void {
  const setDirty = useContext(DraftContext);
  useEffect(() => {
    setDirty(name, dirty);
    return () => { setDirty(name, false); };
  }, [setDirty, name, dirty]);
}
