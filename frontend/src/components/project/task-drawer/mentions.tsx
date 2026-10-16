import type { KeyboardEvent, ReactNode } from 'react';
import { Mentions, Typography, theme } from 'antd';

/** 可以 @ 的人：value 是 user id，label 是顯示名稱 */
export interface MentionCandidate {
  value: string;
  label: string;
}

/**
 * 內文出現「@名字」的成員；送出留言時用來產生 mentionIds。
 * 名字長的先比對，「@王小明」不會因為另一位成員叫「王小」而重複算到。
 */
export function mentionIdsOf(content: string, candidates: readonly MentionCandidate[]): string[] {
  let rest = content;
  const ids: string[] = [];
  for (const candidate of [...candidates].sort((a, b) => b.label.length - a.label.length)) {
    const token = `@${candidate.label}`;
    if (rest.includes(token)) {
      ids.push(candidate.value);
      rest = rest.split(token).join(' ');
    }
  }
  return ids;
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** 留言內文，把 @ 到的人標成主色 */
export function MentionText({ content, names }: { content: string; names: readonly string[] }): ReactNode {
  const { token } = theme.useToken();
  if (names.length === 0) {
    return content;
  }
  const pattern = new RegExp(
    `(${[...names]
      .sort((a, b) => b.length - a.length)
      .map((name) => `@${escapeRegExp(name)}`)
      .join('|')})`,
    'g',
  );
  return content.split(pattern).map((part, index) =>
    index % 2 === 1 ? (
      <Typography.Text key={index} strong style={{ color: token.colorPrimary }}>
        {part}
      </Typography.Text>
    ) : (
      part
    ),
  );
}

interface MentionInputProps {
  value: string;
  onChange: (value: string) => void;
  candidates: readonly MentionCandidate[];
  placeholder?: string;
  autoFocus?: boolean;
  onKeyDown?: (event: KeyboardEvent<HTMLTextAreaElement>) => void;
}

/** 可以輸入 @ 選成員的留言框 */
export function MentionInput({ value, onChange, candidates, placeholder, autoFocus, onKeyDown }: MentionInputProps) {
  return (
    <Mentions
      autoFocus={autoFocus}
      autoSize={{ minRows: 2, maxRows: 8 }}
      maxLength={2000}
      placeholder={placeholder}
      value={value}
      options={candidates.map((candidate) => ({ key: candidate.value, value: candidate.label, label: candidate.label }))}
      notFoundContent="找不到成員"
      onChange={onChange}
      onKeyDown={onKeyDown}
    />
  );
}
