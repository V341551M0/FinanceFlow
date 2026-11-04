import { http } from './http';

export interface Transaction {
  id: number;
  accountId: number;
  accountName: string;
  categoryId: number | null;
  categoryName: string | null;
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'REFUND' | 'ADJUSTMENT';
  direction: 'CREDIT' | 'DEBIT';
  amount: number;
  transactionDate: string;
  description: string;
  source: string;
  status: string;
  notes: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface ListParams {
  page?: number;
  size?: number;
  accountId?: number;
  type?: string;
  from?: string;
  to?: string;
  q?: string;
}

export const transactionsApi = {
  list: (p: ListParams) =>
    http.get<Page<Transaction>>('/transactions', { params: p }).then((r) => r.data),
  remove: (id: number) => http.delete(`/transactions/${id}`).then((r) => r.data),
};