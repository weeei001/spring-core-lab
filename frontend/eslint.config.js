import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'eslint.config.js'] },
  js.configs.recommended,
  // 最嚴格的型別檢查規則：禁止 any、未處理的 Promise、不必要的條件判斷等
  tseslint.configs.strictTypeChecked,
  {
    languageOptions: {
      globals: globals.browser,
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      // 分層規則：其他地方只能從模組的 index.ts（公開 API）引用，不可直接引用模組內部檔案
      'no-restricted-imports': [
        'error',
        { patterns: [{ group: ['@/modules/*/**'], message: '請從模組的 index.ts 引用，例如 @/modules/basic-data' }] },
      ],
    },
  },
);
