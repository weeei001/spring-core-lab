import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import tseslint from 'typescript-eslint';

/** 分層規則：依賴只能往下 app → pages → components → api → shared、core；files 底下的檔案不可引用 forbidden */
const layer = (files, forbidden, message) => ({
  files,
  rules: { 'no-restricted-imports': ['error', { patterns: [{ group: forbidden, message }] }] },
});

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
    },
  },
  layer(['src/pages/**'], ['@/app/**', '@/pages/**'], '頁面之間不互相引用；多個頁面共用的元件請移到 components/'),
  layer(['src/components/**'], ['@/app/**', '@/pages/**'], 'components 不可引用頁面或 app'),
  layer(['src/api/**'], ['@/app/**', '@/pages/**', '@/components/**'], 'api 只能引用 core、shared'),
  layer(['src/core/**', 'src/shared/**'], ['@/app/**', '@/pages/**', '@/components/**', '@/api/**'], 'core、shared 不可引用業務程式'),
);
