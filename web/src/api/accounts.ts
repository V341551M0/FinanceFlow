import { http } from './http';
import type { CreateAccountPayload, FinancialAccount } from '@/types';

export const accountsApi = {
  list: () => http.get<FinancialAccount[]>('/accounts').then((r) => r.data),
  create: (p: CreateAccountPayload) =>
    http.post<FinancialAccount>('/accounts', p).then((r) => r.data),
  remove: (id: number) => http.delete(`/accounts/${id}`).then((r) => r.data),
};