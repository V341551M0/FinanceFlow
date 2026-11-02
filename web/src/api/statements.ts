import { http } from './http';

export interface PreviewRow {
  date: string | null;
  description: string;
  amount: number | null;
  direction: 'CREDIT' | 'DEBIT' | null;
  valid: boolean;
  duplicate: boolean;
  error: string | null;
}

export interface PreviewResponse {
  format: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  duplicateRows: number;
  declaredFinalBalance: number | null;
  projectedBalance: number;
  reconciled: boolean;
  warnings: string[];
  rows: PreviewRow[];
}

export interface ImportResult {
  imported: number;
  skippedDuplicates: number;
  skippedInvalid: number;
  newAccountBalance: number;
  declaredFinalBalance: number | null;
  reconciled: boolean;
}

export const statementsApi = {
  preview: (accountId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return http
      .post<PreviewResponse>(`/statements/preview?accountId=${accountId}`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },
  importFile: (accountId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return http
      .post<ImportResult>(`/statements/import?accountId=${accountId}`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },
};