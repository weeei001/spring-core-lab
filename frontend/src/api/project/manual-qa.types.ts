export type QaResult = 'PASS' | 'FAIL' | 'BLOCKED';
export type QaStatus = QaResult | 'NOT_TESTED';

export interface QaBatch {
  id: number;
  projectId: number;
  versionLabel: string;
  environment: string;
  total: number;
  notTested: number;
  passed: number;
  failed: number;
  blocked: number;
  passRate: number | null;
}

export interface QaExecution {
  id: number;
  itemId: number;
  testerId: string;
  executedAt: string;
  testedVersion: string;
  result: QaResult;
  actual: string;
  bugTaskId: number | null;
}

export interface QaItem {
  id: number;
  batchId: number;
  title: string;
  steps: string;
  expectedResult: string;
  taskId: number | null;
  assigneeId: string | null;
  status: QaStatus;
  latestExecution: QaExecution | null;
}

export interface QaItemInput {
  title: string;
  steps: string;
  expectedResult: string;
  taskId: number | null;
  assigneeId: string | null;
}

export interface QaBatchInput {
  versionLabel: string;
  environment: string;
}

export interface QaExecutionInput {
  testedVersion: string;
  result: QaResult;
  actual: string;
  bugTaskId: number | null;
}
